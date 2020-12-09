package com.adtiming.om.server.cp.dto;

/**
 * Created by huangqiang on 2019/4/2.
 * NoFillReason
 */
public interface NoFillReason {
     int EMPTY_DID = 1;              //empty did
     int COUNTRY_NOT_FOUND = 2;      //country not found
     int NO_GOOGLE_PLAY = 3;         //no google play
     int REGS_DENY = 4;              // Regs deny

     int PUBLISHER_INVALID = 10;     //publisher invalid
     int PUB_APP_INVALID = 20;       //pub app invalid
     int PLACEMENT_INVALID = 21;     //placement invalid
     int MEDIATION_INVALID = 22;     //mediation invalid
     int INSTANCE_NOT_FOUND = 24;    //instance not found
     int INSTANCE_EMPTY = 25;        //instance empty

     int SIZE_INVALID = 30;          //size invalid
     int WIFI_REQUIRED = 40;         //wifi required
     int SDK_VERSION_DENIED = 50;    //sdk version denied
     int OSV_DENIED = 60;            //osv denied
     int MAKE_DENIED = 70;           //make denied
     int BRAND_DENIED = 80;          //brand denied
     int MODEL_DENIED = 90;          //model denied
     int DID_DENIED = 100;           //did denied
     int PERIOD_DENIED = 110;        //period denied
     int RANDOM_NOFILL = 120;        //random nofill
     int DEV_CAMPAIGN_LOST = 130;    //dev_campaign lost
     int DEV_NO_ADT = 131;           //dev_mode not adt
     int DEV_NO_CP = 132;           //dev_mode not cross promotion
     int NO_CAMPAIGN_PC = 140;       //no campaign for plat_country
     int NO_CAMPAIGN_AVALIABLE = 150;//no campaigns avaliable
}
