package org.mobicents.protocols.ss7.sccp.impl.sctp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.mobicents.protocols.ss7.mtp.Mtp3;
import org.mobicents.protocols.ss7.sccp.ActionReference;
import org.mobicents.protocols.ss7.sccp.SccpListener;
import org.mobicents.protocols.ss7.sccp.impl.SccpProviderImpl;
import org.mobicents.protocols.ss7.sccp.impl.parameter.ProtocolClassImpl;
import org.mobicents.protocols.ss7.sccp.impl.ud.UnitDataImpl;
import org.mobicents.protocols.ss7.sccp.impl.ud.XUnitDataImpl;
import org.mobicents.protocols.ss7.sccp.parameter.ProtocolClass;
import org.mobicents.protocols.ss7.sccp.parameter.SccpAddress;
import org.mobicents.protocols.ss7.sccp.ud.UDBase;
import org.mobicents.protocols.ss7.stream.MTPListener;
import org.mobicents.protocols.ss7.stream.MTPProvider;
import org.mobicents.protocols.ss7.stream.MTPProviderFactory;




public class SccpSCTPProviderImpl extends SccpProviderImpl implements MTPListener {
	private static final Logger logger = Logger.getLogger(SccpSCTPProviderImpl.class);

	private MTPProvider mtpProvider;
	private boolean linkUp  =false;

	private int opc;

	private int dpc;

	private int sls;

	private int ssi;

	private int si;

	/** Creates a new instance of SccpProviderImpl */
	public SccpSCTPProviderImpl(Properties props) {
		this.mtpProvider = MTPProviderFactory.getInstance().getProvider(props);
		this.mtpProvider.addMtpListener(this);
		//FIXME: move this to mtp provider?
		this.opc = Integer.parseInt(props.getProperty("sccp.opc"));
        this.dpc = Integer.parseInt(props.getProperty("sccp.dpc"));
        this.sls = Integer.parseInt(props.getProperty("sccp.sls"));
        this.ssi = Integer.parseInt(props.getProperty("sccp.ssf"));
        //this.si  = Integer.parseInt(props.getProperty("sccp.si"));
        this.si = Mtp3._SI_SERVICE_SCCP;
	}



	public void receive(byte[] arg2) {
		// add check for SIO parts?
		// if(logger.isInfoEnabled())
	    //logger.info("Received MSU on L4, service: "+service+",subservice: "+subservice);
		new DeliveryHandler(arg2, super.listener).run();

	}




	public void linkDown() {
		//add more?
		if(linkUp)
		{
			this.linkUp = false;
			if(listener!=null)
					listener.linkDown();
		}
		
	}



	public void linkUp() {
		if(!linkUp)
		{
			this.linkUp = true;
			if(listener!=null)
					listener.linkUp();
		}
		
	}

	public void send(SccpAddress calledParty, SccpAddress callingParty, byte[] data, ActionReference ar) throws IOException {
		
		//FIXME:
		if (this.linkUp) {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ProtocolClass pc = null;
			if(ar instanceof UDBase)
			{
				pc = ((UDBase)ar).getpClass();
			}
			
			if(pc == null)
			{
				pc = new ProtocolClassImpl(0, 0);
			}
			UnitDataImpl unitData = new UnitDataImpl(pc, calledParty, callingParty, data);
			unitData.encode(out);
			byte[] buf = out.toByteArray();
			// this.mtp3.send(si, ssf,buf);
			//this.txBuffer.add(ByteBuffer.wrap(buf));

			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			if(ar != null)
			{
				//we know how to route!
				bos.write(ar.getBackRouteHeader());
				bos.write(buf);
				buf = bos.toByteArray();
			}else
			{
				//we must build it. damn that Dialogic...
				byte[] sif = new byte[5];
				bos.write(sif);
				bos.write(buf);
				buf = bos.toByteArray();
				if(logger.isInfoEnabled())
				{
					logger.info("Sccp TCP Provider default MTP3 Label: DPC["+dpc+"] OPC["+opc+"] SLS["+sls+"] SI["+si+"] SSI["+ssi+"]");
				}
				Mtp3.writeRoutingLabel(buf, si, ssi, sls, dpc, opc);
				
			}
			if(logger.isInfoEnabled())
			{
				logger.info("Sending SCCP buff: "+Arrays.toString(buf));
			}
			this.mtpProvider.send(buf);
		}else
		{
			throw new IOException("Link is not up!");
		}

	}

	public void shutdown() {
		// if(this.mtp3 != null)
		// {
		// this.mtp3.stop();
		// }
		this.mtpProvider.removeMtpListener(this);
		this.mtpProvider.stop();
		this.mtpProvider = null;

	}

	


	



	private static class DeliveryHandler implements Runnable {
		//this is MTP3 message, we need to decode!
		private byte[] msg;
		private SccpListener listener;
		public DeliveryHandler(byte[] msg,SccpListener listener) {
			super();
			this.msg = msg;
			this.listener = listener;
		}

		public void run() {
			try {
				if (this.listener != null) {
					//offset for sif !
					ByteArrayInputStream bin = new ByteArrayInputStream(msg,5,msg.length);
					DataInputStream in = new DataInputStream(bin);
					int mt;

					mt = in.readUnsignedByte();

					switch (mt) {
					case UnitDataImpl._MT:
						UnitDataImpl unitData = new UnitDataImpl();
						unitData.setBackRouteHeader(msg);
						unitData.decode(in);

						this.listener.onMessage(unitData.getCalledParty(), unitData.getCallingParty(), unitData.getData(),unitData);

						break;
					// 0x11
					case XUnitDataImpl._MT:
						XUnitDataImpl xunitData = new XUnitDataImpl();
						xunitData.setBackRouteHeader(msg);
						xunitData.decode(in);

						this.listener.onMessage(xunitData.getCalledParty(), xunitData.getCallingParty(), xunitData.getData(),xunitData);
						break;
					default:
						logger.error("Undefined message type, MT:" + mt+", Message dump: \n"+Arrays.toString(msg));
						
					}
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				logger.error("Failed to pass UD", e);
			}

		}

	}

	public static void main(String[] srgs) {
		

		byte[] MSU = new byte[] { -61, 53, -73, -47, -35, 17, 1, 15, 4, 15, 26, -84, 11, 18, -110, 0, 17, 4, -105, 32, 115, 0, 114, 1, 11, 18, -110, 0, 17, 4, -105, 32, 115, 0
		                                                                                     , 2, 1, -110, 98, -127, -113, 72, 4, -26, 0, 3, 84, 107, 30, 40, 28, 6, 7, 0, 17, -122, 5, 1, 1, 1, -96, 17, 96, 15, -128, 2, 7, -128, -95, 9, 6, 7, 4, 0, 0, 1, 0, 50, 1, 108, -128, -95, 99, 2, 1, 0,
		                                                                                     2, 1, 0, 48, 91, -128, 1, 8, -126, 8, -124, 16, -105, 32, -125, 32, 104, 6, -125, 7, 3, 19, 9, 50, 38, 89, 24, -123, 1, 10, -118, 8, -124, -109, -105, 32, 115, 0, 2, 1, -69, 5, -128, 3, -128, -112, -93, -100, 1, 12, -97, 50, 8, 82, 0, 7, 50, 1, 86, 4, -14, -65, 53, 3, -125, 1, 17, -97, 54, 5, -57, 120, 45, 0, 1, -97, 55, 7, -111, -105, 32, 115, 0, 2, -15, -97, 57, 8, 2, 1, 80, 113, 49, 53, 36, 97,
		                                                                                      0, 0, 18, 1, 6, 0 };

		
		SccpListener lst = new SccpListener() {

			public void onMessage(SccpAddress calledPartyAddress, SccpAddress callingPartyAddress, byte[] data,
					ActionReference backReference) {
				System.err.println(Arrays.toString(data));

			}

			public void linkUp() {
				// TODO Auto-generated method stub

			}

			public void linkDown() {
				// TODO Auto-generated method stub

			}
		};
		DeliveryHandler dh = new DeliveryHandler(MSU, lst);
		dh.run();

								
		byte[] MTP3 = new byte[] { -61, 70, 119, -51, -35, 9, 0, 3, 14, 25, 11, 18, -110, 0, 17, 4, -105, 32, 115, 0, 2, 1, 11, 18, -110, 0, 17, 4, -105, 32, 115, 0, 114, 1,
		                                                                                         -78, 101, -127, -81, 72, 1, 0, 73, 4, -26, 0, 3, 84, 107, 42, 40, 40, 6, 7, 0, 17, -122, 5, 1, 1, 1, -96, 29, 97, 27, -128, 2, 7, -128, -95, 9, 6, 7, 4, 0, 0, 1, 0, 50, 1, -94, 3, 2, 1, 0, -93, 5, -95, 3, 2, 1, 0, 108, 120, -95, 26, 2, 1, 0, 2, 1, 35, 48, 18, -128, 11, -96, 9, -128, 2, 70, 80, -95, 3, 1, 1, 1, -94, 3, -128, 1, 1, -95, 42, 2, 1, 1, 2, 1, 23, 48, 34, -96, 32, 48, 6, -128, 1, 15, -127, 1, 1, 48, 6, -128, 1, 14, -127, 1, 1, 48, 6, -128, 1, 13, -127, 1, 1, 48, 6, -128, 1, 18, -127, 1, 1, -95, 24, 2, 1, 2, 2, 1, 45, 48, 16, -96, 9, 10, 1, 1, 10, 1, 2, 10, 1, 30, -93, 3, -128, 1, 2,
		                                                                                          -95, 20, 2, 1, 3, 2, 1, 20, 48, 12, -96, 10, 4, 8, -124, 16, -105, 32, -125, 32, 104, 6 };
		
		int msu_DPC = Mtp3._getFromSif_DPC(MSU, 1);
		int msu_OPC = Mtp3._getFromSif_OPC(MSU, 1);
		int msu_SLS = Mtp3._getFromSif_SLS(MSU,1);
		int msu_SI = Mtp3._getFromSif_SI(MSU);
		int msu_SSI = Mtp3._getFromSif_SSI(MSU);
		
		
		int mtp_DPC = Mtp3._getFromSif_DPC(MTP3, 1);
		int mtp_OPC = Mtp3._getFromSif_OPC(MTP3, 1);
		int mtp_SLS = Mtp3._getFromSif_SLS(MTP3,1);
		int mtp_SI = Mtp3._getFromSif_SI(MTP3);
		int mtp_SSI = Mtp3._getFromSif_SSI(MTP3);
		System.err.println(msu_DPC+" -- "+mtp_OPC);
		System.err.println(msu_OPC+" -- "+mtp_DPC);
		System.err.println(msu_SLS+" -- "+mtp_SLS);
		System.err.println(msu_SI+" -- "+mtp_SI);
		System.err.println(msu_SSI+" -- "+mtp_SSI);
		
		

		//send back
		byte[] OLD_SENDBACK = new byte[] { 9, 0, 3, 14, 25, 11, 18, -110, 0, 17, 4, -105, 32, 115, 0, 2, 1, 11, 18, -110, 0, 17, 4, -105,
				32, 115, 0, 114,1, 
				//param len
				-75,
				//TCContinue
				101, 
					//len
					-127, 
					-78, 
					//72 - orig TX ID
					72, 
						//len
						4, 
						89, 0, 3, -68, 
					//73 - dest tx id
					73,
						//len
						4, 
						89, 0, 3, -68, 
					//107 -- dialog portion
					107,
						//len
						42, 
					40,  40,   6,   7,   0,  17,-122,   5,   1,   1, 
				  //40,  40,   6,   7,   0,  17,-122,   5,   1,   1,
					 1, -96,  29,  97,  27,-128,   2,   7,-128, -95, 
				   //1, -96,  29,  97,  27,-128,   2,   7,-128, -95, 
					 9,   6,   7,   4,   0,   0,   1,   0,  50,   1, 
				   //9,   6,   7,   4,   0,   0,   1,   0,  50,   1, 
				   -94,   3,   2,   1,   0, -93,   5, -95,   3,   2, 
				 //-94,   3,   2,   1,   0, -93,   5, -95,   3,   2,
				     1, 0,
				  // 1, 0, 
				     
						
						
						
				     
				    //component portion 
					108,
					//len
					120,
						//cmp1
						-95, 
						//len1
						26, 
							2, 1, 1, 2, 1, 35, 48, 18, -128, 11,
						  //2, 1, -128, 2, 1, 35, 48, 18, -128, 11,
							-96, 9, -128, 2, 70, 80, -95, 3, 1, 1,
						  //-96, 9, -128, 2, 70, 80, -95, 3, 1, 1,
							1, -94, 3, -128, 1, 1,
						  //1, -94, 3, -128, 1, 1,
							 
							  
							  	
						//cmp2
					   -95,
					   //len2
					   42, 
					   		2, 1,    2, 2, 1, 23, 48, 34, -96, 32, 
					   	  //2, 1, -127, 2, 1, 23, 48, 34, -96, 32, 
					   		48, 6, -128, 1, 15, -127, 1, 1, 48, 6, 
					   	  //48, 6, -128, 1, 15, -127, 1, 1, 48, 6, 
					   		-128, 1, 14, -127, 1, 1, 48, 6, -128, 1,
					   	  //-128, 1, 14, -127, 1, 1, 48, 6, -128, 1, 
					   		13,	-127, 1, 1, 48, 6, -128, 1, 18, -127,
					   	  //13,	-127, 1, 1, 48, 6, -128, 1, 18, -127, 
					   		1, 1,
					   	  //1, 1, 
							
							
							
							
					   //cmp3
					   -95,
					   //len3
					   24, 
					   		2, 1,    3, 2, 1, 45, 48, 16, -96, 9,
					   	  //2, 1, -126, 2, 1, 45, 48, 16, -96, 9,
					   		10, 1, 1, 10, 1, 2, 10, 1, 30, -93,
					   	  //10, 1, 1, 10, 1, 2, 10, 1, 30, -93, 
					   		3, -128, 1, 2,
					   	  //3, -128, 1, 2,
					   		 
							
							 
					   	//cmp4
					   	-95,
					   	//len4
					   	20, 
					   		2, 1,    4, 2, 1, 20, 48, 12, -96, 10, 
					   	  //2, 1, -125, 2, 1, 20, 48, 12, -96, 10, 
					   		4, 8, -124, 16, -105, 32, -125, 32, 104, 6 };
						  //4, 8, -124, 16, -105, 32, -125, 32, 104, 6 
							 
		
		byte[] NEW_SENDBACK2 = new byte[] { 9, 0, 3, 14, 25, 11, 18, -110, 0, 17, 4, -105, 32, 115, 0, 2, 1, 11, 18, -110, 0, 17, 4, -105, 32, 115, 0, 114, 1,
		                                                                                                   -78, 101, -127, -81, 72, 1, 0, 73, 4, -25, 0, 3, 6, 107, 42, 40, 40, 6, 7, 0, 17, -122, 5, 1, 1, 1, -96, 29, 97, 27, -128, 2, 7, -128, -95, 9, 6, 7, 4, 0, 0, 1, 0, 50, 1, -94, 3, 2, 1, 0, -93, 5, -95,
		                                                                                                   3, 2, 1, 0, 108, 120, -95, 26, 2, 1, 0, 2, 1, 35, 48, 18, -128, 11, -96, 9, -128, 2, 70, 80, -95, 3, 1, 1, 1, -94, 3, -128, 1, 1, -95, 42, 2, 1, 1, 2, 1, 23, 48, 34, -96, 32, 48, 6, -128, 1, 15, -127
		                                                                                                  , 1, 1, 48, 6, -128, 1, 14, -127, 1, 1, 48, 6, -128, 1, 13, -127, 1, 1, 48, 6, -128, 1, 18, -127, 1, 1, -95, 24, 2, 1, 2, 2, 1, 45, 48, 16, -96, 9, 10, 1, 1, 10, 1, 2, 10, 1, 30, -93, 3, -128, 1, 2, -
		                                                                                                  95, 20, 2, 1, 3, 2, 1, 20, 48, 12, -96, 10, 4, 8, -124, 16, -105, 32, -125, 32, 104, 6};

		byte[] NEW_SENDBACK = new byte[] { 9, 0, 3, 14, 25, 11, 18, -110, 0, 17, 4, -105, 32, 115, 0, 2, 1, 11, 18, -110, 0, 17, 4, -105,
				32, 115, 0, 114,1, 
				//param len
				89,
				//TCBegin
				101,
					//LEN
					87,
					//72 - orig TX ID
					72, 
						//LEN
						1, 
						0,
					//DestTxId
					73,
						//len
						4, 
						89, 0, 3, -68, 
					//dialog portion
					107, 42, 
					40, 40,   6,   7,   0,  17,-122,   5,   1,   1,   
					1, -96,  29,  97,  27,-128,   2,   7,-128, -95, 
					9,   6,   7,   4,   0,   0,   1,   0,  50,   1, 
				  -94,   3,   2,   1,   0, -93,   5, -95,   3,   2, 
				    1, 0, 
					//comp portion
					108,
						//len
						32, 
						//cmp1
						-95,
						//len1
						6, 
							2,	1, -128, 2, 1, 35,
						//cmp2
						-95,
						//len2
						6, 
							2, 1, -127, 2, 1, 23,
						//cmp3
						-95,
						//len3
						6, 
							2, 1, -126, 2, 1, 45,
						//cmp4
						-95,
						//len4
						6, 
							2, 1, -126, 2, 1, 45 };
	}
}
