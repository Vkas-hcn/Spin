package com.spin.secure.key

object Constant {
    //日志tag
    const val logTagSpin = "logTagSpin"
    //spin_config
    const val spin_config = "spin_config"
    // vpn配置本地文件名
    const val VPN_BOOT_LOCAL_FILE_NAME_SPIN = "spin_remote.json"
    // Session Json
    const val INSTALL_TYPE_SPIN  = "installTypeSpin"
    //tba上报地址(测试)
    const val TBA_ADDRESS_TEST_SPIN  = "https://test-gimbel.securesuper.net/thatd/mop"

    //tba上报地址(正式)
    const val TBA_ADDRESS_SPIN  = "https://gimbel.securesuper.net/ragout/faust/beset/im"

    //服务器下发地址（测试）
    const val SERVER_DISTRIBUTION_ADDRESS_TEST_SPIN  = "https://test.fastspins.net/wien/slx/"

    //服务器下发地址（正式）
    const val SERVER_DISTRIBUTION_ADDRESS_SPIN  = "https://prod.fastspins.net/wien/slx/"

    //cloak 测试/正式地址
    const val cloak_url_SPIN  = "https://leslie.securesuper.net/thousand/anodic/donna"

    //VPN链接后的IP
    const val IP_AFTER_VPN_LINK_SPIN  = "iPAfterVpnLinkSpin"

    //VPN链接后的城市
    const val IP_AFTER_VPN_CITY_SPIN  = "iPAfterVpnCitySpin"

    //1:打开黑名单用户屏蔽； //2:关闭黑名单用户屏蔽； //默认1
    const val BUBBLE_CLOAK = "bubble_cloak"
    //是否是黑名单用户
    const val BLACKLIST_USER_SPIN  ="blacklistUserSpin"
    //是否获取黑名单用户
    const val WHETHER_TO_OBTAIN_BLACKLISTED_USERS  ="whetherToObtainBlacklistedUsers"

    //下发服务器数据
    const val SEND_SERVER_DATA ="sendServerData"
    //服务器接收成功
    const val serverReceivedSuccess ="serverReceivedSuccess"
    //installReferrer
    const val INSTALL_REFERRER = "installReferrer"

    //当前IP
    const val CURRENT_IP_SPIN = "currentIpSpin"
    //google广告id
    const val GOOGLE_ADVERTISING_ID_SPIN = "googleAdvertisingIdSpin"
    //UUID值
    const val UUID_VALUE_SPIN = "uuidValueSpin"
    // Session Json
    const val SESSION_JSON_SPIN = "sessionJsonSpin"
}