package com.hexagonkt.realworld

import com.hexagonkt.core.Jvm.systemSettingOrNull
import com.hexagonkt.core.LOOPBACK_INTERFACE
import java.net.InetAddress

data class Settings(
    val bindAddress: InetAddress = systemSettingOrNull("bindAddress") ?: LOOPBACK_INTERFACE,
    val bindPort: Int = systemSettingOrNull("bindPort") ?: 2010,
    val keyPairAlias: String = systemSettingOrNull("keyPairAlias") ?: "realWorld",
    val keyStorePassword: String = systemSettingOrNull("keyStorePassword") ?: "storepass",
    val keyStoreResource: String =
        systemSettingOrNull("keyStoreResource") ?: "classpath:keystore.p12",
    val mongodbUrl: String =
        systemSettingOrNull<String>("mongodbUrl") ?: "mongodb://localhost:3010/real_world",
    val mongodbUser: String = systemSettingOrNull<String>("mongodbUser") ?: "root",
    val mongodbPassword: String = systemSettingOrNull<String>("mongodbPassword") ?: "password",
)
