package behav;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.virtualbox_5_0.*;

import agents.Teleporter;
import utils.VBoxInterface;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;

public class MigrateIn extends OneShotBehaviour {
	//private String ip;
	private final String port_num;
	String vm;
	String myIP;
	AID requester;
	
	public MigrateIn (Agent a, String VMname, AID req) {
		super(a);
		//this.ip = IP_Address;
		this.vm = VMname;
		this.port_num = "6000";
		this.requester = req;
	}
	
	public void action() {	
		/*
		ProcessBuilder pb = new ProcessBuilder("VBoxManage", "modifyvm", vm, "--teleporter", "on", "--teleporterport", port_num);
		try {
			Process process_init = pb.start();
			System.out.println("teleport initiated");
		} catch (IOException e) {
			e.printStackTrace();
		}
		ProcessBuilder svm = new ProcessBuilder("VBoxManage", "startvm", vm);
		try {
			Process process_start  = svm.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
		*/
		
		//Enable Teleporting on this host
		IMachine vm = VBoxInterface.getInstance().getMachinebyUUID(this.vm);
		boolean result = VBoxInterface.getInstance().enableTelep(vm);
		
		if (result){
			try {
				VBoxInterface.getInstance().launchMachine(vm);
				//Get Host IP
				this.myIP = InetAddress.getLocalHost().getHostAddress().toString();
			}
			catch (UnknownHostException uh) {
				uh.printStackTrace();
			}
		
			//Inform source host that teleporting is ready here on destination
			ACLMessage inform_ready = new ACLMessage(ACLMessage.AGREE);
			inform_ready.addReceiver(requester);
			String content = this.myIP + ":" + this.vm;
			inform_ready.setContent(content);
			// Waiting for machine to be ready for teleport
			
			((Teleporter) myAgent).migrating = true;
			
			boolean TelepIn = false;
			MachineState state;
			while(!TelepIn){
				state = vm.getState();
				if (state == MachineState.TeleportingIn){
					TelepIn = true; 
				}
			}
			this.myAgent.doWait(2000);
			
			//sending the AGREE message to teleport source
			this.myAgent.send(inform_ready);
			
			//Wait for machine to end teleport stage 
			boolean EndTelep = false;
			while(!EndTelep){
				state = vm.getState();
				if (state != MachineState.TeleportingIn){
					EndTelep = true;
				}
			}
			VBoxInterface.getInstance().disableTelep(vm);
			this.myAgent.doWait(2000);
			((Teleporter) myAgent).migrating = false;
		}
		
		else {
			System.out.println("Teleport wasn't successful :(");
		}
		
		}	
	}

	

