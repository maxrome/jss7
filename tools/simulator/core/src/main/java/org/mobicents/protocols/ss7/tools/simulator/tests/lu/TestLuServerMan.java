/*
 * TeleStax, Open Source Cloud Communications  Copyright 2012.
 * and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.mobicents.protocols.ss7.tools.simulator.tests.lu;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.mobicents.protocols.ss7.map.api.*;
import org.mobicents.protocols.ss7.map.api.errors.MAPErrorMessage;
import org.mobicents.protocols.ss7.map.api.errors.SMEnumeratedDeliveryFailureCause;
import org.mobicents.protocols.ss7.map.api.primitives.*;
import org.mobicents.protocols.ss7.map.api.service.lsm.*;
import org.mobicents.protocols.ss7.map.api.service.mobility.MAPDialogMobility;
import org.mobicents.protocols.ss7.map.api.service.mobility.MAPServiceMobility;
import org.mobicents.protocols.ss7.map.api.service.mobility.MAPServiceMobilityListener;
import org.mobicents.protocols.ss7.map.api.service.mobility.authentication.*;
import org.mobicents.protocols.ss7.map.api.service.mobility.faultRecovery.ForwardCheckSSIndicationRequest;
import org.mobicents.protocols.ss7.map.api.service.mobility.faultRecovery.ResetRequest;
import org.mobicents.protocols.ss7.map.api.service.mobility.faultRecovery.RestoreDataRequest;
import org.mobicents.protocols.ss7.map.api.service.mobility.faultRecovery.RestoreDataResponse;
import org.mobicents.protocols.ss7.map.api.service.mobility.imei.CheckImeiRequest;
import org.mobicents.protocols.ss7.map.api.service.mobility.imei.CheckImeiResponse;
import org.mobicents.protocols.ss7.map.api.service.mobility.locationManagement.*;
import org.mobicents.protocols.ss7.map.api.service.mobility.oam.ActivateTraceModeRequest_Mobility;
import org.mobicents.protocols.ss7.map.api.service.mobility.oam.ActivateTraceModeResponse_Mobility;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.AnyTimeInterrogationRequest;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.AnyTimeInterrogationResponse;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.ProvideSubscriberInfoRequest;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.ProvideSubscriberInfoResponse;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberManagement.*;
import org.mobicents.protocols.ss7.map.api.service.sms.*;
import org.mobicents.protocols.ss7.map.api.service.supplementary.SSCode;
import org.mobicents.protocols.ss7.map.api.service.supplementary.SupplementaryCodeValue;
import org.mobicents.protocols.ss7.map.api.smstpdu.*;
import org.mobicents.protocols.ss7.map.smstpdu.*;
import org.mobicents.protocols.ss7.sccp.parameter.SccpAddress;
import org.mobicents.protocols.ss7.tcap.api.MessageType;
import org.mobicents.protocols.ss7.tcap.asn.comp.Problem;
import org.mobicents.protocols.ss7.tools.simulator.Stoppable;
import org.mobicents.protocols.ss7.tools.simulator.common.AddressNatureType;
import org.mobicents.protocols.ss7.tools.simulator.common.TesterBase;
import org.mobicents.protocols.ss7.tools.simulator.level3.MapMan;
import org.mobicents.protocols.ss7.tools.simulator.level3.MapProtocolVersion;
import org.mobicents.protocols.ss7.tools.simulator.level3.NumberingPlanMapType;
import org.mobicents.protocols.ss7.tools.simulator.management.TesterHost;
import org.mobicents.protocols.ss7.tools.simulator.tests.sms.NumberingPlanIdentificationType;
import org.mobicents.protocols.ss7.tools.simulator.tests.sms.SmsCodingType;
import org.mobicents.protocols.ss7.tools.simulator.tests.sms.TestSmsServerManMBean;
import org.mobicents.protocols.ss7.tools.simulator.tests.sms.TypeOfNumberType;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

/**
 *
 * @author sergey vetyutnev
 *
 */
public class TestLuServerMan extends TesterBase implements TestLuServerManMBean, Stoppable, MAPDialogListener, MAPServiceMobilityListener {

    private static final Logger logger = Logger.getLogger(TestLuServerMan.class);

    public static String SOURCE_NAME = "TestLuServer";

    private final String name;
    private MapMan mapMan;

    private boolean isStarted = false;
    private int countSriReq = 0;
    private int countSriResp = 0;
    private int countMtFsmReq = 0;
    private int countMtFsmResp = 0;
    private int countMoFsmReq = 0;
    private int countMoFsmResp = 0;
    private int countIscReq = 0;
    private int countErrRcvd = 0;
    private int countErrSent = 0;
    private int countRsmdsReq = 0;
    private int countRsmdsResp = 0;
    private int countAscReq = 0;
    private int countAscResp = 0;
    private String currentRequestDef = "";
    private boolean needSendSend = false;
    private boolean needSendClose = false;

    private static Charset isoCharset = Charset.forName("ISO-8859-1");

    public TestLuServerMan() {
        super(SOURCE_NAME);
        this.name = "???";
    }

    public TestLuServerMan(String name) {
        super(SOURCE_NAME);
        this.name = name;
    }

    public void setTesterHost(TesterHost testerHost) {
        this.testerHost = testerHost;
    }

    public void setMapMan(MapMan val) {
        this.mapMan = val;
    }

    @Override
    public AddressNatureType getAddressNature() {
        return new AddressNatureType(this.testerHost.getConfigurationData().getTestSmsServerConfigurationData().getAddressNature().getIndicator());
    }

    @Override
    public String getAddressNature_Value() {
        return new AddressNatureType(this.testerHost.getConfigurationData().getTestSmsServerConfigurationData().getAddressNature().getIndicator()).toString();
    }

    @Override
    public void setAddressNature(AddressNatureType val) {
        this.testerHost.getConfigurationData().getTestSmsServerConfigurationData().setAddressNature(AddressNature.getInstance(val.intValue()));
        this.testerHost.markStore();
    }

    @Override
    public NumberingPlanMapType getNumberingPlan() {
        return new NumberingPlanMapType(this.testerHost.getConfigurationData().getTestSmsServerConfigurationData().getNumberingPlan().getIndicator());
    }

    @Override
    public String getNumberingPlan_Value() {
        return new NumberingPlanMapType(this.testerHost.getConfigurationData().getTestSmsServerConfigurationData().getNumberingPlan().getIndicator())
                .toString();
    }

    @Override
    public void setNumberingPlan(NumberingPlanMapType val) {
        this.testerHost.getConfigurationData().getTestSmsServerConfigurationData().setNumberingPlan(NumberingPlan.getInstance(val.intValue()));
        this.testerHost.markStore();
    }

    @Override
    public String getHLRAddress() {
        return this.testerHost.getConfigurationData().getTestLuServerConfigurationData().getHLRAddress();
    }

    @Override
    public void setHLRAddress(String val) {
        this.testerHost.getConfigurationData().getTestLuServerConfigurationData().setHLRAddress(val);
        this.testerHost.markStore();
    }

    @Override
    public MapProtocolVersion getMapProtocolVersion() {
        return this.testerHost.getConfigurationData().getTestSmsServerConfigurationData().getMapProtocolVersion();
    }

    @Override
    public String getMapProtocolVersion_Value() {
        return this.testerHost.getConfigurationData().getTestSmsServerConfigurationData().getMapProtocolVersion().toString();
    }

    @Override
    public void setMapProtocolVersion(MapProtocolVersion val) {
        this.testerHost.getConfigurationData().getTestSmsServerConfigurationData().setMapProtocolVersion(val);
        this.testerHost.markStore();
    }

    @Override
    public int getHlrSsn() {
        return this.testerHost.getConfigurationData().getTestSmsServerConfigurationData().getHlrSsn();
    }

    @Override
    public void setHlrSsn(int val) {
        this.testerHost.getConfigurationData().getTestSmsServerConfigurationData().setHlrSsn(val);
        this.testerHost.markStore();
    }

    @Override
    public int getVlrSsn() {
        return this.testerHost.getConfigurationData().getTestSmsServerConfigurationData().getVlrSsn();
    }

    @Override
    public void setVlrSsn(int val) {
        this.testerHost.getConfigurationData().getTestSmsServerConfigurationData().setVlrSsn(val);
        this.testerHost.markStore();
    }

    @Override
    public TypeOfNumberType getTypeOfNumber() {
        return new TypeOfNumberType(this.testerHost.getConfigurationData().getTestSmsServerConfigurationData().getTypeOfNumber().getCode());
    }

    @Override
    public String getTypeOfNumber_Value() {
        return new TypeOfNumberType(this.testerHost.getConfigurationData().getTestSmsServerConfigurationData().getTypeOfNumber().getCode()).toString();
    }

    @Override
    public void setTypeOfNumber(TypeOfNumberType val) {
        this.testerHost.getConfigurationData().getTestSmsServerConfigurationData().setTypeOfNumber(TypeOfNumber.getInstance(val.intValue()));
        this.testerHost.markStore();
    }

    @Override
    public NumberingPlanIdentificationType getNumberingPlanIdentification() {
        return new NumberingPlanIdentificationType(this.testerHost.getConfigurationData().getTestSmsServerConfigurationData().getNumberingPlanIdentification()
                .getCode());
    }

    @Override
    public String getNumberingPlanIdentification_Value() {
        return new NumberingPlanIdentificationType(this.testerHost.getConfigurationData().getTestSmsServerConfigurationData().getNumberingPlanIdentification()
                .getCode()).toString();
    }

    @Override
    public void setNumberingPlanIdentification(NumberingPlanIdentificationType val) {
        this.testerHost.getConfigurationData().getTestSmsServerConfigurationData()
                .setNumberingPlanIdentification(NumberingPlanIdentification.getInstance(val.intValue()));
        this.testerHost.markStore();
    }

    @Override
    public SmsCodingType getSmsCodingType() {
        return this.testerHost.getConfigurationData().getTestSmsServerConfigurationData().getSmsCodingType();
    }

    @Override
    public String getSmsCodingType_Value() {
        return this.testerHost.getConfigurationData().getTestSmsServerConfigurationData().getSmsCodingType().toString();
    }

    @Override
    public void setSmsCodingType(SmsCodingType val) {
        this.testerHost.getConfigurationData().getTestSmsServerConfigurationData().setSmsCodingType(val);
        this.testerHost.markStore();
    }

    @Override
    public boolean isSendSrsmdsIfError() {
        return this.testerHost.getConfigurationData().getTestSmsServerConfigurationData().isSendSrsmdsIfError();
    }

    @Override
    public void setSendSrsmdsIfError(boolean val) {
        this.testerHost.getConfigurationData().getTestSmsServerConfigurationData().setSendSrsmdsIfError(val);
        this.testerHost.markStore();
    }

    @Override
    public boolean isGprsSupportIndicator() {
        return this.testerHost.getConfigurationData().getTestSmsServerConfigurationData().isGprsSupportIndicator();
    }

    @Override
    public void setGprsSupportIndicator(boolean val) {
        this.testerHost.getConfigurationData().getTestSmsServerConfigurationData().setGprsSupportIndicator(val);
        this.testerHost.markStore();
    }

    @Override
    public void putAddressNature(String val) {
        AddressNatureType x = AddressNatureType.createInstance(val);
        if (x != null)
            this.setAddressNature(x);
    }

    @Override
    public void putNumberingPlan(String val) {
        NumberingPlanMapType x = NumberingPlanMapType.createInstance(val);
        if (x != null)
            this.setNumberingPlan(x);
    }

    @Override
    public void putMapProtocolVersion(String val) {
        MapProtocolVersion x = MapProtocolVersion.createInstance(val);
        if (x != null)
            this.setMapProtocolVersion(x);
    }

    @Override
    public void putTypeOfNumber(String val) {
        TypeOfNumberType x = TypeOfNumberType.createInstance(val);
        if (x != null)
            this.setTypeOfNumber(x);
    }

    @Override
    public void putNumberingPlanIdentification(String val) {
        NumberingPlanIdentificationType x = NumberingPlanIdentificationType.createInstance(val);
        if (x != null)
            this.setNumberingPlanIdentification(x);
    }

    @Override
    public void putSmsCodingType(String val) {
        SmsCodingType x = SmsCodingType.createInstance(val);
        if (x != null)
            this.setSmsCodingType(x);
    }

    @Override
    public String getCurrentRequestDef() {
        return "LastDialog: " + currentRequestDef;
    }

    @Override
    public String getState() {
        StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        sb.append(SOURCE_NAME);
        sb.append(": ");
        sb.append("<br>Count: countSriReq-");
        sb.append(countSriReq);
        sb.append(", countSriResp-");
        sb.append(countSriResp);
        sb.append("<br>countMtFsmReq-");
        sb.append(countMtFsmReq);
        sb.append(", countMtFsmResp-");
        sb.append(countMtFsmResp);
        sb.append("<br> countMoFsmReq-");
        sb.append(countMoFsmReq);
        sb.append(", countMoFsmResp-");
        sb.append(countMoFsmResp);
        sb.append(", countIscReq-");
        sb.append(countIscReq);
        sb.append("<br>countRsmdsReq-");
        sb.append(countRsmdsReq);
        sb.append(", countRsmdsResp-");
        sb.append(countRsmdsResp);
        sb.append(", countAscReq-");
        sb.append(countAscReq);
        sb.append("<br>countAscResp-");
        sb.append(countAscResp);
        sb.append(", countErrRcvd-");
        sb.append(countErrRcvd);
        sb.append(", countErrSent-");
        sb.append(countErrSent);
        sb.append("</html>");
        return sb.toString();
    }

    public boolean start() {
        this.countSriReq = 0;
        this.countSriResp = 0;
        this.countMtFsmReq = 0;
        this.countMtFsmResp = 0;
        this.countMoFsmReq = 0;
        this.countMoFsmResp = 0;
        this.countIscReq = 0;
        this.countErrRcvd = 0;
        this.countErrSent = 0;
        this.countRsmdsReq = 0;
        this.countRsmdsResp = 0;
        this.countAscReq = 0;
        this.countAscResp = 0;

        MAPProvider mapProvider = this.mapMan.getMAPStack().getMAPProvider();
        mapProvider.getMAPServiceMobility().acivate();
        //mapProvider.getMAPServiceSms().acivate();
        mapProvider.getMAPServiceMobility().addMAPServiceListener(this);


        //mapProvider.getMAPServiceSms().addMAPServiceListener(this);
        mapProvider.addMAPDialogListener(this);
        //mapProvider.addMAPDialogListener(this);
        this.testerHost.sendNotif(SOURCE_NAME, "LSM service has been started", "", Level.INFO);
        isStarted = true;

        return true;
    }

    @Override
    public void stop() {
        MAPProvider mapProvider = this.mapMan.getMAPStack().getMAPProvider();
        isStarted = false;
        mapProvider.getMAPServiceMobility().deactivate();
        mapProvider.getMAPServiceMobility().removeMAPServiceListener(this);
        mapProvider.removeMAPDialogListener(this);
        this.testerHost.sendNotif(SOURCE_NAME, "LSM Server has been stopped", "", Level.INFO);
    }

    @Override
    public void execute() {
    }

    @Override
    public String closeCurrentDialog() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String performSRIForSM(String destIsdnNumber) {
        if (!isStarted)
            return "The tester is not started";
        if (destIsdnNumber == null || destIsdnNumber.equals(""))
            return "DestIsdnNumber is empty";

        currentRequestDef = "";

        return doSendSri(destIsdnNumber, "this.getServiceCenterAddress()", null);
    }

    private String curDestIsdnNumber = null;

    private String doSendSri(String destIsdnNumber, String serviceCentreAddr, MtMessageData messageData) {

        MAPProvider mapProvider = this.mapMan.getMAPStack().getMAPProvider();

        MAPApplicationContextVersion vers;
        switch (this.testerHost.getConfigurationData().getTestSmsServerConfigurationData().getMapProtocolVersion().intValue()) {
        case MapProtocolVersion.VAL_MAP_V1:
            vers = MAPApplicationContextVersion.version1;
            break;
        case MapProtocolVersion.VAL_MAP_V2:
            vers = MAPApplicationContextVersion.version2;
            break;
        default:
            vers = MAPApplicationContextVersion.version3;
            break;
        }
        MAPApplicationContext mapAppContext = MAPApplicationContext.getInstance(MAPApplicationContextName.shortMsgGatewayContext, vers);

        ISDNAddressString msisdn = mapProvider.getMAPParameterFactory().createISDNAddressString(
                this.testerHost.getConfigurationData().getTestSmsServerConfigurationData().getAddressNature(),
                this.testerHost.getConfigurationData().getTestSmsServerConfigurationData().getNumberingPlan(), destIsdnNumber);
        AddressString serviceCentreAddress = mapProvider.getMAPParameterFactory().createAddressString(
                this.testerHost.getConfigurationData().getTestSmsServerConfigurationData().getAddressNature(),
                this.testerHost.getConfigurationData().getTestSmsServerConfigurationData().getNumberingPlan(), serviceCentreAddr);
        curDestIsdnNumber = destIsdnNumber;

        try {
            MAPDialogSms curDialog = mapProvider.getMAPServiceSms()
                    .createNewDialog(
                            mapAppContext,
                            this.mapMan.createOrigAddress(),
                            null,
                            this.mapMan.createDestAddress(destIsdnNumber, this.testerHost.getConfigurationData().getTestSmsServerConfigurationData()
                                    .getHlrSsn()), null);
            HostMessageData hostMessageData = new HostMessageData();
            hostMessageData.mtMessageData = messageData;
            curDialog.setUserObject(hostMessageData);

            curDialog.addSendRoutingInfoForSMRequest(msisdn, true, serviceCentreAddress, null, this.testerHost.getConfigurationData()
                    .getTestSmsServerConfigurationData().isGprsSupportIndicator(), null, null, null);

            // this cap helps us give SCCP error if any
            // curDialog.setReturnMessageOnError(true);

            curDialog.send();

            String sriData = createSriData(curDialog.getLocalDialogId(), destIsdnNumber, serviceCentreAddr);
            currentRequestDef += "Sent SriReq;";
            this.countSriReq++;
            this.testerHost.sendNotif(SOURCE_NAME, "Sent: sriReq", sriData, Level.DEBUG);

            return "SendRoutingInfoForSMRequest has been sent";
        } catch (MAPException ex) {
            return "Exception when sending SendRoutingInfoForSMRequest: " + ex.toString();
        }
    }

    private String createSriData(long dialogId, String destIsdnNumber, String serviceCentreAddr) {
        StringBuilder sb = new StringBuilder();
        sb.append("dialogId=");
        sb.append(dialogId);
        sb.append(", destIsdnNumber=\"");
        sb.append(destIsdnNumber);
        sb.append("\", serviceCentreAddr=\"");
        sb.append(serviceCentreAddr);
        sb.append("\"");
        return sb.toString();
    }

    @Override
    public String performSRIForSM_MtForwardSM(String msg, String destIsdnNumber, String origIsdnNumber) {
        if (!isStarted)
            return "The tester is not started";
        if (origIsdnNumber == null || origIsdnNumber.equals(""))
            return "OrigIsdnNumber is empty";
        if (destIsdnNumber == null || destIsdnNumber.equals(""))
            return "DestIsdnNumber is empty";
        if (msg == null || msg.equals(""))
            return "Msg is empty";
        int maxMsgLen = this.testerHost.getConfigurationData().getTestSmsServerConfigurationData().getSmsCodingType().getSupportesMaxMessageLength(0);
        if (msg.length() > maxMsgLen)
            return "Simulator does not support message length for current encoding type more than " + maxMsgLen;

        currentRequestDef = "";

        MtMessageData mmd = new MtMessageData();
        mmd.msg = msg;
        mmd.origIsdnNumber = origIsdnNumber;

        return doSendSri(destIsdnNumber, "this.getServiceCenterAddress()", mmd);
    }

    @Override
    public String performMtForwardSM(String msg, String destImsi, String vlrNumber, String origIsdnNumber) {
        if (!isStarted)
            return "The tester is not started";
        if (msg == null || msg.equals(""))
            return "Msg is empty";
        if (destImsi == null || destImsi.equals(""))
            return "DestImsi is empty";
        if (vlrNumber == null || vlrNumber.equals(""))
            return "VlrNumber is empty";
        if (origIsdnNumber == null || origIsdnNumber.equals(""))
            return "OrigIsdnNumber is empty";
        int maxMsgLen = this.testerHost.getConfigurationData().getTestSmsServerConfigurationData().getSmsCodingType().getSupportesMaxMessageLength(0);
        if (msg.length() > maxMsgLen)
            return "Simulator does not support message length for current encoding type more than " + maxMsgLen;

        currentRequestDef = "";

        return doMtForwardSM(msg, destImsi, vlrNumber, origIsdnNumber, "this.getServiceCenterAddress()");
    }

    private String doMtForwardSM(String msg, String destImsi, String vlrNumber, String origIsdnNumber, String serviceCentreAddr) {

        MAPProvider mapProvider = this.mapMan.getMAPStack().getMAPProvider();

        MAPApplicationContextVersion vers;
        MAPApplicationContextName acn = MAPApplicationContextName.shortMsgMTRelayContext;
        switch (this.testerHost.getConfigurationData().getTestSmsServerConfigurationData().getMapProtocolVersion().intValue()) {
        case MapProtocolVersion.VAL_MAP_V1:
            vers = MAPApplicationContextVersion.version1;
            acn = MAPApplicationContextName.shortMsgMORelayContext;
            break;
        case MapProtocolVersion.VAL_MAP_V2:
            vers = MAPApplicationContextVersion.version2;
            break;
        default:
            vers = MAPApplicationContextVersion.version3;
            break;
        }
        MAPApplicationContext mapAppContext = MAPApplicationContext.getInstance(acn, vers);

        IMSI imsi = mapProvider.getMAPParameterFactory().createIMSI(destImsi);
        SM_RP_DA da = mapProvider.getMAPParameterFactory().createSM_RP_DA(imsi);
        AddressString serviceCentreAddress = mapProvider.getMAPParameterFactory().createAddressString(
                this.testerHost.getConfigurationData().getTestSmsServerConfigurationData().getAddressNature(),
                this.testerHost.getConfigurationData().getTestSmsServerConfigurationData().getNumberingPlan(), serviceCentreAddr);
        SM_RP_OA oa = mapProvider.getMAPParameterFactory().createSM_RP_OA_ServiceCentreAddressOA(serviceCentreAddress);

        try {
            AddressField originatingAddress = new AddressFieldImpl(
                    this.testerHost.getConfigurationData().getTestSmsServerConfigurationData().getTypeOfNumber(), this.testerHost.getConfigurationData()
                            .getTestSmsServerConfigurationData().getNumberingPlanIdentification(), origIsdnNumber);
            Calendar cld = new GregorianCalendar();
            int year = cld.get(Calendar.YEAR);
            int mon = cld.get(Calendar.MONTH);
            int day = cld.get(Calendar.DAY_OF_MONTH);
            int h = cld.get(Calendar.HOUR);
            int m = cld.get(Calendar.MINUTE);
            int s = cld.get(Calendar.SECOND);
            int tz = cld.get(Calendar.ZONE_OFFSET);
            AbsoluteTimeStamp serviceCentreTimeStamp = new AbsoluteTimeStampImpl(year - 2000, mon, day, h, m, s, tz / 1000 / 60 / 15);

            int dcsVal = 0;
            switch (this.testerHost.getConfigurationData().getTestSmsServerConfigurationData().getSmsCodingType().intValue()) {
            case SmsCodingType.VAL_GSM7:
                dcsVal = 0;
                break;
            case SmsCodingType.VAL_GSM8:
                dcsVal = 4;
                break;
            case SmsCodingType.VAL_UCS2:
                dcsVal = 8;
                break;
            }
            DataCodingScheme dcs = new DataCodingSchemeImpl(dcsVal);

            UserDataHeader udh = null;
            if (dcs.getCharacterSet() == CharacterSet.GSM8) {
                ApplicationPortAddressing16BitAddressImpl apa16 = new ApplicationPortAddressing16BitAddressImpl(16020, 0);
                udh = new UserDataHeaderImpl();
                udh.addInformationElement(apa16);
            }

            UserData userData = new UserDataImpl(msg, dcs, udh, isoCharset);
            ProtocolIdentifier pi = new ProtocolIdentifierImpl(0);
            SmsDeliverTpdu tpdu = new SmsDeliverTpduImpl(false, false, false, false, originatingAddress, pi, serviceCentreTimeStamp, userData);
            SmsSignalInfo si = mapProvider.getMAPParameterFactory().createSmsSignalInfo(tpdu, null);

            MAPDialogSms curDialog = mapProvider.getMAPServiceSms().createNewDialog(mapAppContext, this.mapMan.createOrigAddress(), null,
                    this.mapMan.createDestAddress(vlrNumber, this.testerHost.getConfigurationData().getTestSmsServerConfigurationData().getVlrSsn()), null);

            if (si.getData().length < 110 || vers == MAPApplicationContextVersion.version1) {
                if (this.testerHost.getConfigurationData().getTestSmsServerConfigurationData().getMapProtocolVersion().intValue() <= 2)
                    curDialog.addForwardShortMessageRequest(da, oa, si, false);
                else
                    curDialog.addMtForwardShortMessageRequest(da, oa, si, false, null);
                curDialog.send();

                String mtData = createMtData(curDialog.getLocalDialogId(), destImsi, vlrNumber, origIsdnNumber, serviceCentreAddr);
                currentRequestDef += "Sent mtReq;";
                this.countMtFsmReq++;
                this.testerHost.sendNotif(SOURCE_NAME, "Sent: mtReq: " + msg, mtData, Level.DEBUG);
            } else {
                ResendMessageData md = new ResendMessageData();
                md.da = da;
                md.oa = oa;
                md.si = si;
                md.destImsi = destImsi;
                md.vlrNumber = vlrNumber;
                md.origIsdnNumber = origIsdnNumber;
                md.serviceCentreAddr = serviceCentreAddr;
                md.msg = msg;

                HostMessageData hmd = (HostMessageData) curDialog.getUserObject();
                if (hmd == null) {
                    hmd = new HostMessageData();
                    curDialog.setUserObject(hmd);
                }
                hmd.resendMessageData = md;

                curDialog.send();
                currentRequestDef += "Sent emptTBegin;";
                this.testerHost.sendNotif(SOURCE_NAME, "Sent: emptTBegin", "", Level.DEBUG);
            }

            return "MtForwardShortMessageRequest has been sent";
        } catch (MAPException ex) {
            return "Exception when sending MtForwardShortMessageRequest: " + ex.toString();
        }
    }

    private String createMtData(long dialogId, String destImsi, String vlrNumber, String origIsdnNumber, String serviceCentreAddr) {
        StringBuilder sb = new StringBuilder();
        sb.append("dialogId=");
        sb.append(dialogId);
        sb.append(", destImsi=\"");
        sb.append(destImsi);
        sb.append(", vlrNumber=\"");
        sb.append(vlrNumber);
        sb.append(", origIsdnNumber=\"");
        sb.append(origIsdnNumber);
        sb.append("\", serviceCentreAddr=\"");
        sb.append(serviceCentreAddr);
        sb.append("\"");
        return sb.toString();
    }

    /*
    @Override
    public void onForwardShortMessageRequest(ForwardShortMessageRequest ind) {
        if (!isStarted)
            return;

        MAPDialogSms curDialog = ind.getMAPDialog();
        long invokeId = ind.getInvokeId();
        SM_RP_DA da = ind.getSM_RP_DA();
        SM_RP_OA oa = ind.getSM_RP_OA();
        SmsSignalInfo si = ind.getSM_RP_UI();

        if (da.getServiceCentreAddressDA() != null) { // mo message
            this.onMoRequest(da, oa, si, curDialog);

            try {
                curDialog.addForwardShortMessageResponse(invokeId);
                this.needSendClose = true;

                this.countMoFsmResp++;
                this.testerHost.sendNotif(SOURCE_NAME, "Sent: moResp", "", Level.DEBUG);
            } catch (MAPException e) {
                this.testerHost.sendNotif(SOURCE_NAME, "Exception when invoking addMoForwardShortMessageResponse : " + e.getMessage(), e, Level.ERROR);
            }
        }
    }

    @Override
    public void onForwardShortMessageResponse(ForwardShortMessageResponse ind) {
        if (!isStarted)
            return;

        this.countMtFsmResp++;

        MAPDialogSms curDialog = ind.getMAPDialog();
        long invokeId = curDialog.getLocalDialogId();
        currentRequestDef += "Rsvd mtResp;";
        this.testerHost.sendNotif(SOURCE_NAME, "Rcvd: mtResp", "", Level.DEBUG);

        if (ind.getMAPDialog().getTCAPMessageType() == MessageType.Continue) {
            needSendClose = true;
        }
    }

    @Override
    public void onMoForwardShortMessageRequest(MoForwardShortMessageRequest ind) {
        if (!isStarted)
            return;

        MAPDialogSms curDialog = ind.getMAPDialog();
        long invokeId = ind.getInvokeId();
        SM_RP_DA da = ind.getSM_RP_DA();
        SM_RP_OA oa = ind.getSM_RP_OA();
        SmsSignalInfo si = ind.getSM_RP_UI();

        this.onMoRequest(da, oa, si, curDialog);

        try {
            curDialog.addMoForwardShortMessageResponse(invokeId, null, null);
            this.needSendClose = true;

            this.countMoFsmResp++;
            this.testerHost.sendNotif(SOURCE_NAME, "Sent: moResp", "", Level.DEBUG);
        } catch (MAPException e) {
            this.testerHost.sendNotif(SOURCE_NAME, "Exception when invoking addMoForwardShortMessageResponse : " + e.getMessage(), e, Level.ERROR);
        }
    }

    private void onMoRequest(SM_RP_DA da, SM_RP_OA oa, SmsSignalInfo si, MAPDialogSms curDialog) {

        this.countMoFsmReq++;

        si.setGsm8Charset(isoCharset);
        String serviceCentreAddr = null;
        if (da != null) {
            AddressString as = da.getServiceCentreAddressDA();
            if (as != null)
                serviceCentreAddr = as.getAddress();
        }

        String origIsdnNumber = null;
        if (oa != null) {
            ISDNAddressString isdn = oa.getMsisdn();
            if (isdn != null)
                origIsdnNumber = isdn.getAddress();
        }

        try {
            String msg = null;
            String destIsdnNumber = null;
            if (si != null) {
                SmsTpdu tpdu = si.decodeTpdu(true);
                if (tpdu instanceof SmsSubmitTpdu) {
                    SmsSubmitTpdu dTpdu = (SmsSubmitTpdu) tpdu;
                    AddressField af = dTpdu.getDestinationAddress();
                    if (af != null)
                        destIsdnNumber = af.getAddressValue();
                    UserData ud = dTpdu.getUserData();
                    if (ud != null) {
                        ud.decode();
                        msg = ud.getDecodedMessage();

                        UserDataHeader udh = ud.getDecodedUserDataHeader();
                        if (udh != null) {
                            StringBuilder sb = new StringBuilder();
                            sb.append("[");
                            int i2 = 0;
                            for (byte b : udh.getEncodedData()) {
                                int i1 = (b & 0xFF);
                                if (i2 == 0)
                                    i2 = 1;
                                else
                                    sb.append(", ");
                                sb.append(i1);
                            }
                            sb.append("] ");
                            msg = sb.toString() + msg;
                        }
                    }
                }
            }
            String uData = this.createMoData(curDialog.getLocalDialogId(), destIsdnNumber, origIsdnNumber, serviceCentreAddr);
            this.testerHost.sendNotif(SOURCE_NAME, "Rcvd: moReq: " + msg, uData, Level.DEBUG);
        } catch (MAPException e) {
            this.testerHost.sendNotif(SOURCE_NAME, "Exception when decoding MoForwardShortMessageRequest tpdu : " + e.getMessage(), e, Level.ERROR);
        }
    }
    */

    /*
    private String createMoData(long dialogId, String destIsdnNumber, String origIsdnNumber, String serviceCentreAddr) {
        StringBuilder sb = new StringBuilder();
        sb.append("dialogId=");
        sb.append(dialogId);
        sb.append(", destIsdnNumber=\"");
        sb.append(destIsdnNumber);
        sb.append(", origIsdnNumber=\"");
        sb.append(origIsdnNumber);
        sb.append("\", serviceCentreAddr=\"");
        sb.append(serviceCentreAddr);
        sb.append("\"");
        return sb.toString();
    }
    */

    /* sms listener methods
    @Override
    public void onMoForwardShortMessageResponse(MoForwardShortMessageResponse moForwSmRespInd) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onMtForwardShortMessageRequest(MtForwardShortMessageRequest mtForwSmInd) {
        // TODO Auto-generated method stub

    }


    @Override
    public void onMtForwardShortMessageResponse(MtForwardShortMessageResponse ind) {
        if (!isStarted)
            return;

        this.countMtFsmResp++;

        MAPDialogSms curDialog = ind.getMAPDialog();
        long invokeId = curDialog.getLocalDialogId();
        currentRequestDef += "Rsvd mtResp;";
        this.testerHost.sendNotif(SOURCE_NAME, "Rcvd: mtResp", "", Level.DEBUG);

        if (ind.getMAPDialog().getTCAPMessageType() == MessageType.Continue) {
            needSendClose = true;
        }
    }

    @Override
    public void onSendRoutingInfoForSMRequest(SendRoutingInfoForSMRequest sendRoutingInfoForSMInd) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onSendRoutingInfoForSMResponse(SendRoutingInfoForSMResponse ind) {
        if (!isStarted)
            return;

        this.countSriResp++;

        MAPDialogSms curDialog = ind.getMAPDialog();
        long invokeId = curDialog.getLocalDialogId();
        LocationInfoWithLMSI li = ind.getLocationInfoWithLMSI();
        String vlrNum = "";
        if (li != null && li.getNetworkNodeNumber() != null)
            vlrNum = li.getNetworkNodeNumber().getAddress();
        currentRequestDef += "Rsvd SriResp;";
        String destImsi = "";
        if (ind.getIMSI() != null)
            destImsi = ind.getIMSI().getData();
        String uData = this.createSriRespData(invokeId, ind);
        this.testerHost.sendNotif(SOURCE_NAME, "Rcvd: sriResp", uData, Level.DEBUG);

        if (curDialog.getUserObject() != null && vlrNum != null && !vlrNum.equals("") && destImsi != null && !destImsi.equals("")) {
            HostMessageData hmd = (HostMessageData) curDialog.getUserObject();
            MtMessageData mmd = hmd.mtMessageData;
            if (mmd != null) {
                mmd.vlrNum = vlrNum;
                mmd.destImsi = destImsi;
            }

            // // sending SMS
            // doMtForwardSM(mmd.msg, destImsi, vlrNum, mmd.origIsdnNumber,
            // this.testerHost.getConfigurationData().getTestSmsServerConfigurationData()
            // .getServiceCenterAddress());
        }
    }
    end sms listener methods*/

    /*
    private String createSriRespData(long dialogId, SendRoutingInfoForSMResponse ind) {
        StringBuilder sb = new StringBuilder();
        sb.append("dialogId=");
        sb.append(dialogId);
        sb.append(", ind=\"");
        sb.append(ind);
        sb.append("\"");
        return sb.toString();
    }

    private String createIscReqData(long dialogId, MWStatus mwStatus) {
        StringBuilder sb = new StringBuilder();
        sb.append("dialogId=");
        sb.append(dialogId);
        sb.append(",\n mwStatus=");
        sb.append(mwStatus);
        sb.append(",\n");
        return sb.toString();
    }
    */

    /* sms listener service
    @Override
    public void onReportSMDeliveryStatusRequest(ReportSMDeliveryStatusRequest reportSMDeliveryStatusInd) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onReportSMDeliveryStatusResponse(ReportSMDeliveryStatusResponse ind) {
        if (!isStarted)
            return;

        this.countRsmdsResp++;

        MAPProvider mapProvider = this.mapMan.getMAPStack().getMAPProvider();
        MAPDialogSms curDialog = ind.getMAPDialog();
        long invokeId = ind.getInvokeId();

        this.testerHost.sendNotif(SOURCE_NAME, "Rcvd: rsmdsResp", ind.toString(), Level.DEBUG);
    }

    @Override
    public void onInformServiceCentreRequest(InformServiceCentreRequest ind) {
        if (!isStarted)
            return;

        this.countSriResp++;
        currentRequestDef += "Rsvd IscReq;";

        MAPDialogSms curDialog = ind.getMAPDialog();
        long invokeId = curDialog.getLocalDialogId();
        MWStatus mwStatus = ind.getMwStatus();
        String uData = this.createIscReqData(invokeId, mwStatus);
        this.testerHost.sendNotif(SOURCE_NAME, "Rcvd: iscReq", uData, Level.DEBUG);
    }

    @Override
    public void onAlertServiceCentreRequest(AlertServiceCentreRequest ind) {
        if (!isStarted)
            return;

        this.countAscReq++;

        MAPProvider mapProvider = this.mapMan.getMAPStack().getMAPProvider();
        MAPDialogSms curDialog = ind.getMAPDialog();
        long invokeId = ind.getInvokeId();

        this.testerHost.sendNotif(SOURCE_NAME, "Rcvd: ascReq", ind.toString(), Level.DEBUG);

        try {
            if (curDialog.getApplicationContext().getApplicationContextVersion() == MAPApplicationContextVersion.version1) {
                curDialog.release();
            } else {
                curDialog.addAlertServiceCentreResponse(invokeId);

                this.countAscResp++;
                this.testerHost.sendNotif(SOURCE_NAME, "Sent: ascResp", "", Level.DEBUG);

                this.needSendClose = true;
            }

        } catch (MAPException e) {
            this.testerHost.sendNotif(SOURCE_NAME, "Exception when invoking addAlertServiceCentreResponse() : " + e.getMessage(), e, Level.ERROR);
        }
    }

    @Override
    public void onAlertServiceCentreResponse(AlertServiceCentreResponse alertServiceCentreInd) {
        // TODO Auto-generated method stub

    }


    @Override
    public void onForwardShortMessageResponse(ForwardShortMessageResponse ind) {
        if (!isStarted)
            return;

        this.countMtFsmResp++;

        MAPDialogSms curDialog = ind.getMAPDialog();
        long invokeId = curDialog.getLocalDialogId();
        currentRequestDef += "Rsvd mtResp;";
        this.testerHost.sendNotif(SOURCE_NAME, "Rcvd: mtResp", "", Level.DEBUG);

        if (ind.getMAPDialog().getTCAPMessageType() == MessageType.Continue) {
            needSendClose = true;
        }
    }
    */


    @Override
    public void onDialogRequest(MAPDialog arg0, AddressString arg1, AddressString arg2, MAPExtensionContainer arg3) {
        int i1 = 0;
    }

    @Override
    public void onDialogDelimiter(MAPDialog mapDialog) {

        if (mapDialog.getApplicationContext().getApplicationContextName() == MAPApplicationContextName.shortMsgMTRelayContext
                || mapDialog.getApplicationContext().getApplicationContextName() == MAPApplicationContextName.shortMsgMORelayContext) {
            if (mapDialog.getUserObject() != null) {
                HostMessageData hmd = (HostMessageData) mapDialog.getUserObject();
                ResendMessageData md = hmd.resendMessageData;
                if (md != null) {
                    try {
                        MAPDialogSms dlg = (MAPDialogSms) mapDialog;

                        if (dlg.getApplicationContext().getApplicationContextVersion().getVersion() <= 2)
                            dlg.addForwardShortMessageRequest(md.da, md.oa, md.si, false);
                        else
                            dlg.addMoForwardShortMessageRequest(md.da, md.oa, md.si, null, null);
                        mapDialog.send();

                        String mtData = createMtData(mapDialog.getLocalDialogId(), md.destImsi, md.vlrNumber, md.origIsdnNumber, md.serviceCentreAddr);
                        currentRequestDef += "Rcvd emptTCont;Sent moReq;";
                        this.countMoFsmReq++;
                        this.testerHost.sendNotif(SOURCE_NAME, "Rcvd: emptTCont", "", Level.DEBUG);
                        this.testerHost.sendNotif(SOURCE_NAME, "Sent: moReq: " + md.msg, mtData, Level.DEBUG);
                    } catch (Exception e) {
                        this.testerHost.sendNotif(SOURCE_NAME, "Exception when invoking close() : " + e.getMessage(), e, Level.ERROR);
                        return;
                    }
                    hmd.resendMessageData = null;
                    return;
                }
            }
        }

        try {
            if (needSendSend) {
                needSendSend = false;
                mapDialog.send();
                return;
            }
        } catch (Exception e) {
            this.testerHost.sendNotif(SOURCE_NAME, "Exception when invoking send() : " + e.getMessage(), e, Level.ERROR);
            return;
        }
        try {
            if (needSendClose) {
                needSendClose = false;
                mapDialog.close(false);
                return;
            }
        } catch (Exception e) {
            this.testerHost.sendNotif(SOURCE_NAME, "Exception when invoking close() : " + e.getMessage(), e, Level.ERROR);
            return;
        }

        if (mapDialog.getApplicationContext().getApplicationContextName() == MAPApplicationContextName.shortMsgMTRelayContext
                || mapDialog.getApplicationContext().getApplicationContextName() == MAPApplicationContextName.shortMsgMORelayContext) {
            // this is an empty first TC-BEGIN for MO SMS
            try {
                mapDialog.send();
                currentRequestDef += "Rcvd emptTBeg;Sent emptTCont;";
                this.testerHost.sendNotif(SOURCE_NAME, "Rcvd: emptTBeg", "", Level.DEBUG);
                this.testerHost.sendNotif(SOURCE_NAME, "Sent: emptTCont", "", Level.DEBUG);
            } catch (Exception e) {
                this.testerHost.sendNotif(SOURCE_NAME, "Exception when invoking send() : " + e.getMessage(), e, Level.ERROR);
            }
            return;
        }
    }

    @Override
    public void onDialogClose(MAPDialog mapDialog) {
        if (mapDialog.getUserObject() != null) {
            HostMessageData hmd = (HostMessageData) mapDialog.getUserObject();
            MtMessageData mmd = hmd.mtMessageData;
            if (mmd != null && mmd.vlrNum != null && mmd.destImsi != null) {
                // sending SMS
                doMtForwardSM(mmd.msg, mmd.destImsi, mmd.vlrNum, mmd.origIsdnNumber, this.testerHost.getConfigurationData().getTestSmsServerConfigurationData()
                        .getServiceCenterAddress());
            }
        }

        try {
            if (needSendSend) {
                needSendSend = false;
                mapDialog.send();
            }
        } catch (Exception e) {
            this.testerHost.sendNotif(SOURCE_NAME, "Exception when invoking send() : " + e.getMessage(), e, Level.ERROR);
        }
        try {
            if (needSendClose) {
                needSendClose = false;
                mapDialog.close(false);
            }
        } catch (Exception e) {
            this.testerHost.sendNotif(SOURCE_NAME, "Exception when invoking close() : " + e.getMessage(), e, Level.ERROR);
        }
    }

    @Override
    public void onErrorComponent(MAPDialog dlg, Long invokeId, MAPErrorMessage msg) {
        // if an error for (mt)ForwardSM or SRI requests
        if (dlg.getApplicationContext().getApplicationContextName() != MAPApplicationContextName.shortMsgMTRelayContext
                || dlg.getApplicationContext().getApplicationContextName() != MAPApplicationContextName.shortMsgMORelayContext
                || (dlg.getUserObject() != null && ((HostMessageData) dlg.getUserObject()).mtMessageData != null && ((HostMessageData) dlg.getUserObject()).mtMessageData.msg != null)) {
            if (this.testerHost.getConfigurationData().getTestSmsServerConfigurationData().isSendSrsmdsIfError() && curDestIsdnNumber != null) {
                try {
                    MAPProvider mapProvider = this.mapMan.getMAPStack().getMAPProvider();
                    MAPApplicationContextVersion vers = dlg.getApplicationContext().getApplicationContextVersion();
                    MAPApplicationContext mapAppContext = MAPApplicationContext.getInstance(MAPApplicationContextName.shortMsgGatewayContext, vers);

                    MAPDialogSms curDialog = mapProvider.getMAPServiceSms().createNewDialog(
                            mapAppContext,
                            this.mapMan.createOrigAddress(),
                            null,
                            this.mapMan.createDestAddress(curDestIsdnNumber, this.testerHost.getConfigurationData().getTestSmsServerConfigurationData()
                                    .getHlrSsn()), null);

                    ISDNAddressString msisdn = mapProvider.getMAPParameterFactory().createISDNAddressString(
                            this.testerHost.getConfigurationData().getTestSmsServerConfigurationData().getAddressNature(),
                            this.testerHost.getConfigurationData().getTestSmsServerConfigurationData().getNumberingPlan(), curDestIsdnNumber);
                    AddressString serviceCentreAddress = mapProvider.getMAPParameterFactory().createAddressString(
                            this.testerHost.getConfigurationData().getTestSmsServerConfigurationData().getAddressNature(),
                            this.testerHost.getConfigurationData().getTestSmsServerConfigurationData().getNumberingPlan(), "this.getServiceCenterAddress()");
                    curDestIsdnNumber = null;

                    SMDeliveryOutcome sMDeliveryOutcome = null;
                    if (vers.getVersion() >= 2) {
                        if (msg.isEmSMDeliveryFailure()
                                && msg.getEmSMDeliveryFailure().getSMEnumeratedDeliveryFailureCause() == SMEnumeratedDeliveryFailureCause.memoryCapacityExceeded)
                            sMDeliveryOutcome = SMDeliveryOutcome.memoryCapacityExceeded;
                        else
                            sMDeliveryOutcome = SMDeliveryOutcome.absentSubscriber;
                    }

                    curDialog.addReportSMDeliveryStatusRequest(msisdn, serviceCentreAddress, sMDeliveryOutcome, null, null, false, false, null, null);
                    curDialog.send();

                    currentRequestDef += "Sent RsmdsReq;";
                    this.countRsmdsReq++;
                    String rsmdsData = "msisdn=" + msisdn + ", serviceCentreAddress=" + serviceCentreAddress + ", sMDeliveryOutcome=" + sMDeliveryOutcome;
                    this.testerHost.sendNotif(SOURCE_NAME, "Sent: rsmdsReq", rsmdsData, Level.DEBUG);
                } catch (MAPException e) {
                    this.testerHost.sendNotif(SOURCE_NAME, "Exception when invoking reportSMDeliveryStatusRequest : " + e.getMessage(), e, Level.ERROR);
                }
            }
        }

        super.onErrorComponent(dlg, invokeId, msg);

        // needSendClose = true;
    }

    @Override
    public void onRejectComponent(MAPDialog mapDialog, Long invokeId, Problem problem, boolean isLocalOriginated) {
        super.onRejectComponent(mapDialog, invokeId, problem, isLocalOriginated);
        if (isLocalOriginated)
            needSendClose = true;
    }


    MAPDialogMobility updateLocationDialog;

    /* Monility listener methods */

    @Override
    public void onUpdateLocationRequest(UpdateLocationRequest ind){
        if (!isStarted)
            return;





        MAPDialogMobility curDialog = ind.getMAPDialog();

        //save this dialog, it will be used for updateLocationResponse
        updateLocationDialog = curDialog;






        long invokeId = ind.getInvokeId();

        IMSI imsi = ind.getImsi();
        ISDNAddressString vlrNumber = ind.getVlrNumber();
        ISDNAddressString mscNumber = ind.getMscNumber();


        logger.info("IMSI="+imsi.toString());
        logger.info("VLR_NUMBER=" + vlrNumber.toString());
        logger.info("MSC_NUMBER=" + mscNumber.toString());

        String userData = "IMSI="+imsi.toString() + " VLR_NUMBER=" + vlrNumber.toString() + " MSC_NUMBER=" + mscNumber.toString();

        this.testerHost.sendNotif(SOURCE_NAME, "Rcvd: UpdateLocationRequest", userData, Level.INFO);





        MAPProvider mapProvider = this.mapMan.getMAPStack().getMAPProvider();
        SccpAddress destAddr = this.mapMan.createDestAddress(ind.getVlrNumber().getAddress(), 7);
        SccpAddress origAddr = this.mapMan.createOrigAddress();


        //SccpAddress callingPartyAddress = this.testerHost.getSccpMan().createCallingPartyAddress();
        //SccpAddress calledPartyAddress = this.testerHost.getSccpMan().createCalledPartyAddress(vlrNumber.toString(),7);
        //SccpAddress calledPartyAddress = this.testerHost.getSccpMan().createCalledPartyAddress();


        /*
        MAPDialogMobility newDialog;

        try {
            //create the new dialog
            newDialog = mapProvider.getMAPServiceMobility().createNewDialog(getNetworkLocUpContext(), origAddr, null, destAddr, null);
        }
        catch(MAPException e){
            this.testerHost.sendNotif(SOURCE_NAME, "Exception when creating new dialog for send InsertSubscribeDataRequest : " + e.getMessage(), e, Level.ERROR);
            e.printStackTrace();
            return;

        }
        */


        /*
        this.testerHost.getConfigurationData().getSccpConfigurationData().getNumberingPlan();
        this.testerHost.getConfigurationData().getSccpConfigurationData().getNatureOfAddress();
        this.testerHost.getConfigurationData().getTestLuServerConfigurationData().getHLRAddress();
        */




        //TODO msisdn da parametrizzare, magari nella form di configurazione del test
        ISDNAddressString msisdn = mapProvider.getMAPParameterFactory().createISDNAddressString(AddressNature.international_number,
                NumberingPlan.ISDN, "393203350745");


        Category category = mapProvider.getMAPParameterFactory().createCategory(CategoryValue.ordinaryCallingSubscriber);

        ArrayList<ExtBearerServiceCode> bearerServiceList = new ArrayList<ExtBearerServiceCode>();
        bearerServiceList.add(mapProvider.getMAPParameterFactory().createExtBearerServiceCode(BearerServiceCodeValue.dataPDS_2400bps));
        bearerServiceList.add(mapProvider.getMAPParameterFactory().createExtBearerServiceCode(BearerServiceCodeValue.allSpeechFollowedByDataCDA));

        ArrayList<ExtTeleserviceCode> teleserviceList = new ArrayList<ExtTeleserviceCode>();
        teleserviceList.add(mapProvider.getMAPParameterFactory().createExtTeleserviceCode(TeleserviceCodeValue.emergencyCalls));
        teleserviceList.add(mapProvider.getMAPParameterFactory().createExtTeleserviceCode(TeleserviceCodeValue.allShortMessageServices));
        teleserviceList.add(mapProvider.getMAPParameterFactory().createExtTeleserviceCode(TeleserviceCodeValue.telephony));

        ArrayList<ExtSSInfo> provisionedSS = new ArrayList<ExtSSInfo>();


        SSCode ssCode = mapProvider.getMAPParameterFactory().createSSCode(SupplementaryCodeValue.allForwardingSS);
        ExtTeleserviceCode teleserviceCode = mapProvider.getMAPParameterFactory().createExtTeleserviceCode(TeleserviceCodeValue.telephony);
        ExtBasicServiceCode basicServiceCode = mapProvider.getMAPParameterFactory().createExtBasicServiceCode(teleserviceCode);
        ExtSSStatus ssStatus = mapProvider.getMAPParameterFactory().createExtSSStatus(true,false,true,false);

        ISDNAddressString forwardedToNumber = mapProvider.getMAPParameterFactory().createISDNAddressString(AddressNature.international_number,NumberingPlan.ISDN,"393331231234");

        //ExtForwOptions forwOptions = mapProvider.getMAPParameterFactory().

        ExtForwFeature forwFeature = mapProvider.getMAPParameterFactory().createExtForwFeature(basicServiceCode,ssStatus,forwardedToNumber,null,null,null,null,null);

        ArrayList<ExtForwFeature> extForwFeatures = new ArrayList<ExtForwFeature>();
        extForwFeatures.add(forwFeature);

        ExtForwInfo extForwInfo = mapProvider.getMAPParameterFactory().createExtForwInfo(ssCode,extForwFeatures,null);

        ExtSSInfo extSSInfo = mapProvider.getMAPParameterFactory().createExtSSInfo(extForwInfo);




        provisionedSS.add(extSSInfo);




        //ArrayList<ZoneCode> regionalSubscriptionData = new ArrayList<ZoneCode>();
        ArrayList<ZoneCode> regionalSubscriptionData = null;

        //ArrayList<VoiceBroadcastData> vbsSubscriptionData = new ArrayList<VoiceBroadcastData>();
        ArrayList<VoiceBroadcastData> vbsSubscriptionData = null;

        //ArrayList<VoiceGroupCallData> vgcsSubscriptionData = new ArrayList<VoiceGroupCallData>();
        ArrayList<VoiceGroupCallData> vgcsSubscriptionData = null;


        VlrCamelSubscriptionInfo vlrCamelSubscriptionInfo = null;


        SccpAddress callingPartyAddress = this.testerHost.getSccpMan().createCallingPartyAddress();
//        SccpAddress calledPartyAddress = this.testerHost.getSccpMan().createCalledPartyAddress(vlrNumber.toString(),7);
//        SccpAddress calledPartyAddress = this.testerHost.getSccpMan().createCalledPartyAddress();

        curDialog.setLocalAddress(callingPartyAddress);


        try{
            //newDialog.addInsertSubscriberDataRequest(null,
            curDialog.addInsertSubscriberDataRequest(null,
                    msisdn,
                    category,
                    SubscriberStatus.serviceGranted,
                    bearerServiceList,
                    teleserviceList,
                    provisionedSS,
                    null,
                    false,
                    regionalSubscriptionData,
                    vbsSubscriptionData,
                    vgcsSubscriptionData,
                    null);


            curDialog.send();
            //newDialog.send();
            this.testerHost.sendNotif(SOURCE_NAME, "Sent: InsertSubscribeDataRequest", "", Level.INFO);
        } catch (MAPException e) {
            this.testerHost.sendNotif(SOURCE_NAME, "Exception when invoking addInsertSubscribeDataRequest : " + e.getMessage(), e, Level.ERROR);
        }

    }

    @Override
    public void onUpdateLocationResponse(UpdateLocationResponse ind) {

    }

    @Override
    public void onCancelLocationRequest(CancelLocationRequest request) {

    }

    @Override
    public void onCancelLocationResponse(CancelLocationResponse response) {
        //TODO
    }

    @Override
    public void onSendIdentificationRequest(SendIdentificationRequest request) {

    }

    @Override
    public void onSendIdentificationResponse(SendIdentificationResponse response) {

    }

    @Override
    public void onUpdateGprsLocationRequest(UpdateGprsLocationRequest request) {

    }

    @Override
    public void onUpdateGprsLocationResponse(UpdateGprsLocationResponse response) {

    }

    @Override
    public void onPurgeMSRequest(PurgeMSRequest request) {
        //TODO
    }

    @Override
    public void onPurgeMSResponse(PurgeMSResponse response) {

    }

    @Override
    public void onSendAuthenticationInfoRequest(SendAuthenticationInfoRequest ind) {

        if (!isStarted)
            return;

        this.testerHost.sendNotif(SOURCE_NAME, "Rcvd: sendAuthenticationInfoRequest", "", Level.INFO);

        MAPDialogMobility curDialog = ind.getMAPDialog();

        long invokeId = ind.getInvokeId();

        MAPProvider mapProvider = this.mapMan.getMAPStack().getMAPProvider();

        byte[] rand = new byte[16];
        byte[] sres = new byte[4];
        byte[] kc = new byte[8];

        ArrayList<AuthenticationTriplet> authTripletList = new ArrayList<AuthenticationTriplet>();
        AuthenticationTriplet authTriplet = mapProvider.getMAPParameterFactory().createAuthenticationTriplet(rand, sres, kc);
        authTripletList.add(authTriplet);

        TripletList tripletList = mapProvider.getMAPParameterFactory().createTripletList(authTripletList);


        AuthenticationSetList authSetList = mapProvider.getMAPParameterFactory().createAuthenticationSetList(tripletList);

        //useful to reply with an error messge
        //mapProvider.getMAPErrorMessageFactory().createMAPErrorMessageSMDeliveryFailure()


        SccpAddress callingPartyAddress = this.testerHost.getSccpMan().createCallingPartyAddress();
        curDialog.setLocalAddress(callingPartyAddress);

        try{
            curDialog.addSendAuthenticationInfoResponse(invokeId, authSetList, null, null);
            curDialog.close(false);
            this.testerHost.sendNotif(SOURCE_NAME, "Sent: sendAuthenticationInfoResponse", "", Level.INFO);
        } catch (MAPException e) {
            this.testerHost.sendNotif(SOURCE_NAME, "Exception when invoking addSendAuthenticationInfoResponse : " + e.getMessage(), e, Level.ERROR);
        }




    }

    @Override
    public void onSendAuthenticationInfoResponse(SendAuthenticationInfoResponse ind) {

    }

    @Override
    public void onAuthenticationFailureReportRequest(AuthenticationFailureReportRequest ind) {

    }

    @Override
    public void onAuthenticationFailureReportResponse(AuthenticationFailureReportResponse ind) {

    }

    @Override
    public void onResetRequest(ResetRequest ind) {

    }

    @Override
    public void onForwardCheckSSIndicationRequest(ForwardCheckSSIndicationRequest ind) {

    }

    @Override
    public void onRestoreDataRequest(RestoreDataRequest ind) {
        //TODO
    }

    @Override
    public void onRestoreDataResponse(RestoreDataResponse ind) {

    }

    @Override
    public void onAnyTimeInterrogationRequest(AnyTimeInterrogationRequest request) {

    }

    @Override
    public void onAnyTimeInterrogationResponse(AnyTimeInterrogationResponse response) {

    }

    @Override
    public void onProvideSubscriberInfoRequest(ProvideSubscriberInfoRequest request) {

    }

    @Override
    public void onProvideSubscriberInfoResponse(ProvideSubscriberInfoResponse response) {

    }

    @Override
    public void onInsertSubscriberDataRequest(InsertSubscriberDataRequest request) {

    }

    @Override
    public void onInsertSubscriberDataResponse(InsertSubscriberDataResponse response) {
        if (!isStarted)
            return;

        this.testerHost.sendNotif(SOURCE_NAME, "Rcvd: insertSubscriberDataResponse", "", Level.INFO);

        MAPDialogMobility curDialog = response.getMAPDialog();



        long invokeId = response.getInvokeId();

        MAPProvider mapProvider = this.mapMan.getMAPStack().getMAPProvider();

        if (response.getBearerServiceList() != null) {
            for (ExtBearerServiceCode bs : response.getBearerServiceList()) {


                logger.info("BearerService: " + bs.getBearerServiceCodeValue().toString());
            }
        }

        logger.info("ODBGeneralData: " + response.getODBGeneralData());

        logger.info("Supported camel phases: " + response.getSupportedCamelPhases());

        //this.testerHost.getConfigurationData().getTestLuServerConfigurationData().getHlrSsn()

        ISDNAddressString hlrNumber = mapProvider.getMAPParameterFactory().createISDNAddressString(AddressNature.international_number,
                NumberingPlan.ISDN, this.testerHost.getConfigurationData().getTestLuServerConfigurationData().getHLRAddress());



        try {

//            updateLocationDialog.addUpdateLocationResponse(invokeId, hlrNumber, null, false, false);
//            updateLocationDialog.send();

            curDialog.addUpdateLocationResponse(invokeId, hlrNumber, null, false, false);
            //curDialog.send();
            curDialog.close(false);
            this.testerHost.sendNotif(SOURCE_NAME, "Sent: UpdateLocationResponse", "", Level.INFO);
        } catch (MAPException e) {
            this.testerHost.sendNotif(SOURCE_NAME, "Exception when invoking addUpdateLocationResponse : " + e.getMessage(), e, Level.ERROR);
        }


    }

    @Override
    public void onDeleteSubscriberDataRequest(DeleteSubscriberDataRequest request) {

    }

    @Override
    public void onDeleteSubscriberDataResponse(DeleteSubscriberDataResponse request) {
        //TODO
    }

    @Override
    public void onCheckImeiRequest(CheckImeiRequest request) {

    }

    @Override
    public void onCheckImeiResponse(CheckImeiResponse response) {

    }

    @Override
    public void onActivateTraceModeRequest_Mobility(ActivateTraceModeRequest_Mobility ind) {

    }

    @Override
    public void onActivateTraceModeResponse_Mobility(ActivateTraceModeResponse_Mobility ind) {

    }


    private MAPApplicationContext getSubscriberDataMngtContext(){
        MAPApplicationContextVersion vers;
        //MAPApplicationContextName acn = MAPApplicationContextName.shortMsgMTRelayContext;
        MAPApplicationContextName acn = MAPApplicationContextName.subscriberDataMngtContext;
        //MAPApplicationContextName acn = MAPApplicationContextName.subscriberDataMngtContext;
        switch (this.testerHost.getConfigurationData().getTestLuServerConfigurationData().getMapProtocolVersion().intValue()) {
            case MapProtocolVersion.VAL_MAP_V1:
                vers = MAPApplicationContextVersion.version1;
                //acn = MAPApplicationContextName.shortMsgMORelayContext;
                break;
            case MapProtocolVersion.VAL_MAP_V2:
                vers = MAPApplicationContextVersion.version2;
                break;
            default:
                vers = MAPApplicationContextVersion.version3;
                break;
        }
        MAPApplicationContext mapAppContext = MAPApplicationContext.getInstance(acn, vers);
        return  mapAppContext;
    }

    private MAPApplicationContext getNetworkLocUpContext(){
        MAPApplicationContextVersion vers;
        //MAPApplicationContextName acn = MAPApplicationContextName.shortMsgMTRelayContext;
        MAPApplicationContextName acn = MAPApplicationContextName.subscriberDataMngtContext;
        //MAPApplicationContextName acn = MAPApplicationContextName.networkLocUpContext;
        switch (this.testerHost.getConfigurationData().getTestLuServerConfigurationData().getMapProtocolVersion().intValue()) {
            case MapProtocolVersion.VAL_MAP_V1:
                vers = MAPApplicationContextVersion.version1;
                //acn = MAPApplicationContextName.shortMsgMORelayContext;
                break;
            case MapProtocolVersion.VAL_MAP_V2:
                vers = MAPApplicationContextVersion.version2;
                break;
            default:
                vers = MAPApplicationContextVersion.version3;
                break;
        }
        MAPApplicationContext mapAppContext = MAPApplicationContext.getInstance(acn, vers);
        return  mapAppContext;
    }




    private class HostMessageData {
        public MtMessageData mtMessageData;
        public ResendMessageData resendMessageData;
    }

    private class MtMessageData {
        public String msg;
        public String origIsdnNumber;
        public String vlrNum;
        public String destImsi;
    }

    private class ResendMessageData {
        public SM_RP_DA da;
        public SM_RP_OA oa;
        public SmsSignalInfo si;
        public String msg;
        public String destImsi;
        public String vlrNumber;
        public String origIsdnNumber;
        public String serviceCentreAddr;
    }


}
