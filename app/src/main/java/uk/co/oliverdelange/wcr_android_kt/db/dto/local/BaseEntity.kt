package uk.co.oliverdelange.wcr_android_kt.db.dto.local

import com.google.firebase.firestore.DocumentReference

abstract class BaseEntity {
    abstract var id: Long //Room rowid
    //    abstract var uuid: String //id - not currently used
    abstract var firebaseId: DocumentReference? //Firebase document ID
    abstract var uploadedAt: Long
    abstract var uploaderId: String
}