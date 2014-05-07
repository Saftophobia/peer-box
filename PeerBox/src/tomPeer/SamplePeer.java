package tomPeer;

import handlers.FileHandler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.util.Collection;
import java.util.UUID;

import javax.sound.midi.Track;

import utils.Constants;
import net.tomp2p.connection.Bindings;
import net.tomp2p.connection.PeerConnection;
import net.tomp2p.futures.FutureBootstrap;
import net.tomp2p.futures.FutureDHT;
import net.tomp2p.futures.FutureResponse;
import net.tomp2p.futures.FutureTracker;
import net.tomp2p.p2p.Peer;
import net.tomp2p.p2p.PeerMaker;
import net.tomp2p.peers.Number160;
import net.tomp2p.peers.PeerAddress;
import net.tomp2p.rpc.ObjectDataReply;
import net.tomp2p.storage.Data;
import net.tomp2p.storage.TrackerData;

public class SamplePeer {
	private Peer samplePeer;
	private String peerID;
	private String shareFolderPath;

	// Create an identity
	// Listen for incoming connections
	// Bootstrap to a known peer in the P2P network
	// Start application specific communication

	public SamplePeer(InetAddress IPAddress, String bootstrapIP,
			String shareFolderPath) throws IOException {
		this.setShareFolderPath(shareFolderPath);

		// port and peerID need to set somehow
		// edit: createHash takes SHA1, so UUID can be used
		// random UUID must be stored on hdd after creation
		// new bindings, beyefham el interface lewa7do, (dont supply eth0 or
		// eth1)
		samplePeer = new PeerMaker(Number160.createHash(UUID.randomUUID()
				.toString())).setPorts(Constants.PORT)
				.setBindings(new Bindings(IPAddress)).makeAndListen();

		if (bootstrapIP != null
				&& !bootstrapIP.replaceAll("\\s+", "").isEmpty()) {
			// bootstrap to well-known IP
			if (!this.bootstrapper(bootstrapIP)) {
				System.out.println("failed!");
				// peer.shutdown(); // proper peer shutdown
			}
		}

		// add myself to DHT
		this.putPeerAddress(this.samplePeer.getPeerAddress());

		// open peer for connections, somehow
		this.manageMessages();
	}

	/**
	 * direct messages between peers
	 * 
	 * @author saftophobia
	 */
	private void manageMessages() {
		ObjectDataReply objectDataReply = new ObjectDataReply() {

			@Override
			public Object reply(PeerAddress sender, Object request)
					throws Exception {
				// TODO Auto-generated method stub
				if (request != null && request instanceof SampleMessage) {
					// Presumably we have different type of messages and we need
					// to differ between them, for example, it can call
					// "downloadfile" etc
					switch (request.toString()) {
					case "blah":
						;
						break;
					case "blah1": // download file masalan
						;
						break;
					case "blah2":
						;
						break;
					}
				}
				return null;
			}
		};

		// TODO Auto-generated method stub
		this.samplePeer.setObjectDataReply(objectDataReply);
	}

	private boolean bootstrapper(String bootstrapIP) {
		InetAddress bootstrapAddress = null;
		try {
			// get the InetAddress representation of the bootstrapIP (as in
			// tutorial)
			bootstrapAddress = InetAddress.getByName(bootstrapIP);
		} catch (Exception e) {
			return false;
		}

		FutureBootstrap future = this.samplePeer.bootstrap()
				.setInetAddress(bootstrapAddress).setPorts(Constants.PORT)
				.start();
		future.awaitUninterruptibly();

		if (future.isFailed()) {

			System.out.println("failure");
			return false;
		}

		return true;
	}

	private void checkForUpdates() {

	}

	// might be boolean to make sure its successfull
	private void downloadFile(String hashPart, String filePath) {
		// has to be initiliazed for the tracker
		Number160 locationKey = Number160.createHash(hashPart);

		// sample codes on internet uses .setDomainKey(domainKey), no clue what
		// it does.
		FutureTracker futureTracker = this.samplePeer.getTracker(locationKey)
				.start();

		futureTracker.awaitUninterruptibly();

		// get all trackers
		Collection<TrackerData> trackers = futureTracker.getTrackers();
		FutureResponse futureResponse = null;

		if (trackers != null && !trackers.isEmpty()) {
			while (trackers.iterator().hasNext()) {
				PeerAddress peerAddress = trackers.iterator().next()
						.getPeerAddress();

				if (peerAddress != null) {
					// found an address, initiate connection
					PeerConnection peerConnection = this.samplePeer
							.createPeerConnection(peerAddress, 25);
					if (peerConnection != null) {
						futureResponse = this.samplePeer.sendDirect()
								.setConnection(peerConnection)
								.setObject("some protocol to be decided later")
								.start();
						futureResponse.awaitUninterruptibly();
						break; //3shan matgebsh trackers tanyeen malohmsh lazma
					}
				}
			}
		} else {
			System.out.println("no trackers online");
		}

		// check for response
		if (futureResponse != null && futureResponse.isSuccess()) {
			byte[] datastream = (byte[]) futureResponse.getObject();
			if (datastream != null) {
				// save
				File file = new File(filePath);
				if (file.exists()) {
					// replace it somehow
				} else {
					try {
						FileHandler.createFile(filePath, datastream);

						System.out.println("SUCCESS!");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			} else {
				System.out.println("file download unsuccessful");
			}
		}
	}

	private void reportNewFile(String f) {

	}

	/**
	 * add an address to the DHT, so other peers can communicate with me ( 7asab
	 * mana fahem) peeraddress A PeerAddress contains the node ID and how to
	 * contact this node using both udp and tcp. edit: had to implement the
	 * getpeeraddress to get the tweak. i'll need to put the data in a
	 * serializable form
	 * 
	 * @param peerAddress
	 * @author saftophobia
	 */
	private void putPeerAddress(PeerAddress peerAddress) {
		FutureDHT fdht;
		ByteArrayOutputStream byteArrayOutputStream = null;
		try {
			byteArrayOutputStream = new ByteArrayOutputStream();
			ObjectOutputStream objectOutputStream = new ObjectOutputStream(
					byteArrayOutputStream);

			objectOutputStream.writeObject(peerAddress);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		byte[] byteArrayOutput = byteArrayOutputStream.toByteArray();

		// okay we need to "put" into the dht using the tutorial command
		// edit: the data used to be TEST, now i m gonna PUT the new
		// serializable
		try {
			fdht = this.samplePeer.put(peerAddress.getID())
					.setData(new Data(byteArrayOutput)).start();

			// dont wait forever like the tutorial, set a time
			fdht.wait(3000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch bloc
			e.printStackTrace();
		}

	}

	/**
	 * retrieve address/object from the DHT, 7asab mana fahem bardo
	 * 
	 * @param hashInput
	 * @return
	 * @author saftophobia
	 */
	private PeerAddress getPeerAddress(String hashInput) {
		FutureDHT futureDHT = this.samplePeer.get(new Number160(hashInput))
				.start();
		futureDHT.awaitUninterruptibly();
		if (futureDHT.isSuccess()) {
			Data data = futureDHT.getData();

			try {
				// Data is Serializable
				ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
						data.getData());

				ObjectInputStream objectInputStream = new ObjectInputStream(
						byteArrayInputStream);

				PeerAddress peerAddress = (PeerAddress) objectInputStream
						.readObject();

				// I should authenticate the peerAddress b4 returning, but wtv
				return peerAddress;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return null;

	}

	/**
	 * setters and getters ShareFolder's should be customized
	 * ***********************************************
	 * 
	 * @return
	 */
	public String getShareFolderPath() {
		return shareFolderPath;
	}

	public void setShareFolderPath(String shareFolderPath) {
		this.shareFolderPath = shareFolderPath;
	}

	public Peer getPeer() {
		return samplePeer;
	}

	public void setPeer(Peer peer) {
		this.samplePeer = peer;
	}

	public String getPeerID() {
		return peerID;
	}

	public void setPeerID(String peerID) {
		this.peerID = peerID;
	}

}
