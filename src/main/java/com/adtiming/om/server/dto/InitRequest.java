// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.dto;

import java.util.List;

public class InitRequest extends CommonRequest {

    private long btime;            // uptime
    private long ram;              // RAM size, unit B
    private String country;
    private List<AdNetwork> adns;
    private Android android;
    private IOS ios;

    public static class AdNetwork {
        public int id;            // AdNetwork ID
        public String adapterv;   // AdNetwork Adapter Version
        public String sdkv;       // AdNetwork SDK Version
    }

    public static class Android {
        public String device;                // Build.DEVICE
        public String product;               // Build.PRODUCT
        public int screen_density;           // Pixel density. [0,1,2]
        public int screen_size;              // Pixel density. [1,2,3,4]
        public String cpu_abi;               // ro.product.cpu.abi
        public String cpu_abi2;              // ro.product.cpu.abi2
        public String cpu_abilist;           // ro.product.cpu.abilist
        public String cpu_abilist32;         // ro.product.cpu.abilist32
        public String cpu_abilist64;         // ro.product.cpu.abilist64
        public int api_level;                // Android API Level
        public int d_dpi;                    // DisplayMetrics.densityDpi
        public int dim_size;                 // WebViewBridge.getScreenMetrics().size
        public String xdp;                   // DisplayMetrics.xdpi
        public String ydp;                   // DisplayMetrics.ydpi
        public String dfpid;                 // deviceFingerPrintId, getUniquePsuedoID
        public String time_zone;             // TimeZone.getDefault().getID()
        public String arch;                  // ro.arch
        public String chipname;              // ro.chipname
        public String bridge;                // ro.dalvik.vm.native.bridge
        public String nativebridge;          // persist.sys.nativebridge
        public String bridge_exec;           // ro.enable.native.bridge.exec
        public String isax86_features;       // dalvik.vm.isa.x86.features
        public String isa_x86_variant;       // dalvik.vm.isa.x86.variant
        public String zygote;                // ro.zygote
        public String mock_location;         // ro.allow.mock.location
        public String isa_arm;               // ro.dalvik.vm.isa.arm
        public String isa_arm_features;      // dalvik.vm.isa.arm.features
        public String isa_arm_variant;       // dalvik.vm.isa.arm.variant
        public String isa_arm64_features;    // dalvik.vm.isa.arm64.features
        public String isa_arm64_variant;     // dalvik.vm.isa.arm64.variant
        public String build_user;            // ro.build.user
        public String kernel_qemu;           // ro.kernel.qemu
        public String hardware;              // ro.hardware
        public int sensor_size;              // sensor size
        public List<Sensor> sensors;         // sensor list
        public AppSource as;                 // app source
        public String fb_id;                 // FacebookID
        public int tdm;                      // User total disk size, unit M
    }

    public static class Sensor {
        public int sT;             // sensor type
        public String sV;          //
        public List<Float> sVE;    //
        public List<Float> sVS;    //
        public String sN;          //
    }

    public static class AppSource {
        public int os;            // System comes with installed quantity
        public int gp;            // GooglePlay installed quantity
        public int other;         // Non-GooglePlay installed quantity
    }

    public static class IOS {
        public String idfv;            // iOS IDFV
        public String cpu_type;        // CPU type
        public int cpu_64bits;         // Whether the CPU architecture is 64-bit, 0: No, 1: Yes
        public int cpu_count;          // CPU number
        public int ui_width;           // UI wide, non-screen pixel wide
        public int ui_height;          // UI high, non-screen pixels high
        public float ui_scale;         // ui_screen
        public String hardware_name;   // Hardware name
        public String device_name;     // device name
        public String cf_network;      //
        public String darwin;          // Darwin version
        public String ra;              // Network format
        public String local_id;        // currentLocale.localeIdentifier
        public String tz_ab;           // systemTimeZone.abbreviation
        public String tdsg;            // Total capacity GB
        public String rdsg;            // Available capacity GB
    }

    public long getBtime() {
        return btime;
    }

    public void setBtime(long btime) {
        this.btime = btime;
    }

    public long getRam() {
        return ram;
    }

    public void setRam(long ram) {
        this.ram = ram;
    }

    public String getCountry() {
        return country == null ? super.getCountry() : country;
    }

    // test pass through
    public void setCountry(String country) {
        this.country = country;
    }

    public List<AdNetwork> getAdns() {
        return adns;
    }

    public void setAdns(List<AdNetwork> adns) {
        this.adns = adns;
    }

    public Android getAndroid() {
        return android;
    }

    public void setAndroid(Android android) {
        this.android = android;
    }

    public IOS getIos() {
        return ios;
    }

    public void setIos(IOS ios) {
        this.ios = ios;
    }
}
