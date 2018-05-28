package uk.co.oliverdelange.wcr_android_kt.model

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMarshaller

// Todo generify for all enums
class LocationTypeMarshaller : DynamoDBMarshaller<LocationType> {
    override fun marshall(getterReturnResult: LocationType): String {
        return getterReturnResult.name
    }

    override fun unmarshall(clazz: Class<LocationType>, obj: String): LocationType {
        return LocationType.valueOf(obj)
    }

}