import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;

public class SampleTxCase {

    /*
     * This code shows how to test a very simple case on your code. It's
     * recommended that you think of more involved scenarios. Many things are
     * simplified for the purpose of testing.
     * 
     * We have two transactions tx1, and tx2. Assume tx1 is valid, and its
     * output is already in the UTXO pool. tx2 tries to spend that output. If
     * tx2 provides a valid signature, and does not try to spend more than the
     * output value, while specifying everything correctly it should be
     * considered valid.
     */

    public static void main(String[] args) throws NoSuchAlgorithmException,
            InvalidKeyException, SignatureException, NoSuchProviderException {

        // Create an empty pool
        UTXOPool pool = new UTXOPool();

        /*
         * create some transaction tx1, that will have one of its outputs in the
         * pool. We will assume that this one was valid.
         */
        Transaction tx1 = new Transaction();

        /*
         * Let's specify the inputs and outputs of tx1. Since we assume already
         * that tx1 is valid, there is no need really to add a proper input
         * here. Compare that when we define tx2 later.
         */
        tx1.addInput(null, 0);

        /*
         * To add an output to the transaction, we need to have the public key
         * of a recipient.
         *
         * Here we are going to generate a key pair. The public key is going to
         * be used by tx1 to specify the recipient, and the private key will be
         * used by the recipient to sign tx2 in order to spend the money.
         * This generation step should happen on the second side, but this is
         * just simulating everything on one machine)
         */

        KeyPair keyPair = generateNewKeyPair();

        // specify an output of value 10, and the public key
        tx1.addOutput(10.0, keyPair.getPublic());
        // needed to compute the id of tx1
        tx1.finalize();

        // let's add this UTXO to the pool
        pool.addUTXO(new UTXO(tx1.getHash(), 0), tx1.getOutput(0));

        // Instantiate the TXHandler
        TxHandler txHandler = new TxHandler(pool);

        /************** SPENDING TX1's output ****************/

        // now let's try to spend the previous UTXO by another transaction tx2
        Transaction tx2 = new Transaction();

        // One input of tx2 must refer to the UTXO above (hash, idx)
        tx2.addInput(tx1.getHash(), 0);

        // try to send 9.0 coins to some other public key
        KeyPair keyPair2 = generateNewKeyPair();
        tx2.addOutput(9.0, keyPair2.getPublic());

        /*
         * let's sign with our private key to prove the ownership of the
         * first public key specified by the tx1 output
         */
        byte[] sig = sign(keyPair.getPrivate(), tx2.getRawDataToSign(0));
        tx2.addSignature(sig, 0);
        tx2.finalize();

        /*
         * Now let's try to see if tx2 is valid:
         * This should return true in your code, but that's easy; you should
         * think of more involved scenarios: many transactions, what could go
         * wrong, .. etc.
         */
        System.out.println("Transaction valid? " + txHandler.isValidTx(tx2));

        /*
         * The previous code only checks the validity. To update the
         * pool, your implementation of handleTxs() will be called.
         */

    }

    public static KeyPair generateNewKeyPair() throws NoSuchAlgorithmException, NoSuchProviderException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        return keyGen.genKeyPair();
    }

    public static byte[] sign(PrivateKey privKey, byte[] message)
            throws NoSuchAlgorithmException, SignatureException,
            InvalidKeyException {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privKey);
        signature.update(message);
        return signature.sign();
    }

}