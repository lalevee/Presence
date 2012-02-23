package fr.emse.tscserver;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import de.datenzone.tpm4java.*;

public class TSCPrivacyCa {

    static private TssLowlevel tcs = new TSSCoreService(null);

    private KeyPair rsaKey;

    private byte[] label;

    /**
     * Create a TSCPrivacyCA with a given RSA key pair and a label (the label
     * is relevant since it needs to be given to MakeIdentity when generating an
     * AIK)
     * 
     * @param rsaKey
     * @param label
     */
    public TSCPrivacyCa(KeyPair rsaKey, byte[] label) {
        this.rsaKey = rsaKey;
        this.label = label;
    }

    public void writeToFile(String filename) throws IOException {
        FileOutputStream fos = new FileOutputStream(filename);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(rsaKey);
        oos.writeObject(label);
        oos.close();
    }

    public static TSCPrivacyCa loadCa(String filename) throws IOException,
            ClassNotFoundException {
        FileInputStream fis = new FileInputStream(filename);
        ObjectInputStream ois = new ObjectInputStream(fis);
        KeyPair rsaKey = (KeyPair) ois.readObject();
        byte[] label = (byte[]) ois.readObject();
        return new TSCPrivacyCa(rsaKey, label);
    }

    /**
     * Get the chosenID hash (depending on <b>label</b> and the private key)
     * 
     * @return the chosenID hash
     * @throws NoSuchAlgorithmException
     */
    public byte[] getChosenID() throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA1");
        sha1.update(label);
        return sha1.digest(rsaKey.getPrivate().getEncoded());
    }

    /**
     * process an IdentityRequest created by a TSS implementation of a TPM, and
     * create an IdentityCredential that can be used by the requesting party.
     * 
     * @param req
     *            the request created by a TSS of some remote TPM
     * @param pubEk
     *            the public part of the TPM's endorsement key - our reply is
     *            encrypted to this EK - certificates that state that this EK is
     *            in fact an EK of some TPM should be provided and verified in
     *            this structure.
     * @return an IdentityCredential structure, which contains (encrypted) all
     *         the relevant information to activate the identity
     * @throws InvalidKeyException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidAlgorithmParameterException
     * @throws InvalidKeySpecException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws SignatureException
     * @throws TSSException
     */
    public TCPAIdentityCredential processIdentityRequest(
            TCPAIdentityRequest req, TPMPubKeyWrapper pubEk)
            throws InvalidKeyException, NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidAlgorithmParameterException,
            InvalidKeySpecException, IllegalBlockSizeException,
            BadPaddingException, SignatureException, TSSException {

        TPMSymmetricKey decrSKey = new TPMSymmetricKey(tcs.TSS_RSA_Decrypt(
                rsaKey.getPrivate(), req.getAsymBlob()));
        SecretKey sessionKey;
        Cipher sCipher;
        IvParameterSpec iv;

        switch (req.getSymParms().getAlgorithm()) {
        case TssLowlevel.AlgorithmId.TPM_ALG_3DES:
            sessionKey = new SecretKeySpec(decrSKey.getKey(), "DESede");
            iv = new IvParameterSpec(req.getSymParms().getRaw_parms());
            sCipher = Cipher.getInstance("DESede/CBC/PKCS5Padding");
            sCipher.init(Cipher.DECRYPT_MODE, sessionKey, iv);

            break;
        case TssLowlevel.AlgorithmId.TPM_ALG_AES128:
        case TssLowlevel.AlgorithmId.TPM_ALG_AES196:
        case TssLowlevel.AlgorithmId.TPM_ALG_AES256:
            sessionKey = new SecretKeySpec(decrSKey.getKey(), "AES");
            iv = new IvParameterSpec(req.getSymParms().getRaw_parms());
            sCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            sCipher.init(Cipher.DECRYPT_MODE, sessionKey, iv);

            break;

        default:
            return null;
        }

        TCPAIdentityProof proof = new TCPAIdentityProof(sCipher.doFinal(req
                .getSymBlob()));

        byte[] pubKey = proof.getIdentityKey().getBlobForHashing(false);
        ByteBuffer b = ByteBuffer.allocate(4 + 4 + 20 + pubKey.length);
        b.order(TssLowlevel.TPM_BYTE_ORDER);
        b.putInt(proof.getVersion());
        b.putInt(121); // ordinal of MakeIdentity
        b.put(getChosenID());
        b.put(pubKey);

        PublicKey pub = proof.getIdentityKey().getRSAPubKey();

        Signature sig_v = Signature.getInstance("SHA1withRSA");
        sig_v.initVerify(pub);
        sig_v.update(b.array());

        if (!sig_v.verify(proof.getIdentityBinding()))
            throw new TSSException("Signature verification failed!");

        /*
         * Here, a real Privacy CA would verify that the Endorsement-, Platform-
         * and Conformance-Credential are valid.
         * 
         * Additionally, a Certificate for the Identity Key would be created.
         * Instead, we just return the String "You have been officially
         * certified."
         */

        // generate a session key
        KeyGenerator kgen;
        switch (req.getSymParms().getAlgorithm()) {
        case TssLowlevel.AlgorithmId.TPM_ALG_3DES:
            kgen = KeyGenerator.getInstance("DESede");
            kgen.init(168);
            sessionKey = kgen.generateKey();
            iv = new IvParameterSpec(tcs.GetRandomBytes(8));
            sCipher = Cipher.getInstance("DESede/CBC/PKCS5Padding");
            sCipher.init(Cipher.ENCRYPT_MODE, sessionKey, iv);

            break;
        case TssLowlevel.AlgorithmId.TPM_ALG_AES128:
            kgen = KeyGenerator.getInstance("AES");
            kgen.init(128);
            sessionKey = kgen.generateKey();
            iv = new IvParameterSpec(tcs.GetRandomBytes(16));
            sCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            sCipher.init(Cipher.ENCRYPT_MODE, sessionKey, iv);

            break;
        }

        byte[] encrCertificate = sCipher
                .doFinal("You have been officially certified!".getBytes());
        TPMSymmetricKey sKey = new TPMSymmetricKey(req.getSymParms()
                .getAlgorithm(), TssLowlevel.EncScheme.ENCSCHEME_NONE,
                sessionKey.getEncoded());

        // compute a hash of the public identity key, so that the tpm can be
        // sure this is for the right key

        byte[] idKeyDigest = TSSCoreService.TSS_sha1(proof.getIdentityKey()
                .getBlobForHashing(false));
        // put all this together
        byte[] caContents = sKey.getTPM_ASYM_CA_CONTENTS(idKeyDigest);

        // encrypt it to the EK
        byte[] encrSessionKey = tcs.TSS_RSA_Encrypt(pubEk.getRSAPubKey(),
                caContents);

        TPMKeyParms credParm = req.getSymParms();
        credParm.setRaw_parms(iv.getIV());
        return new TCPAIdentityCredential(encrSessionKey, encrCertificate,
                credParm);
    }

    /**
     * @return the label of this privacy CA
     */
    public byte[] getLabel() {
        return label;
    }
    
    /**
     * @return the public key
     */
    public PublicKey getPublicKey() {
    	return rsaKey.getPublic();
    }
}
