/*
 * This file is an extended version of the file provided 
 * with the assignment handout to cover more cases.
 * Before running this file make sure you add your print
 * statements and try different transactions to handler in different order.
 * Keep in mind that using the isValidTx() doesn't affect the utxo pool and 
 * only handleTxs() does.
 */

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;

public class SampleTxCase {

    public static void main(String[] args) throws NoSuchAlgorithmException,
            InvalidKeyException, SignatureException, NoSuchProviderException {

        // Create an empty pool
        UTXOPool pool = new UTXOPool();
        Transaction tx1 = new Transaction();
        tx1.addInput(null, 0);
        KeyPair keyPair = generateNewKeyPair();

        // specify an output of value 25.0, and the public key
        tx1.addOutput(25.0, keyPair.getPublic());
        // needed to compute the id of tx1
        tx1.finalize();

        // let's add this UTXO to the pool
        pool.addUTXO(new UTXO(tx1.getHash(), 0), tx1.getOutput(0));

        TxHandler txHandler = new TxHandler(pool);

        /************** SPENDING TX1's output ****************/

        // now let's try to spend the previous UTXO by another transaction tx2
        Transaction tx2 = new Transaction();

        // One input of tx2 must refer to the UTXO above (hash, idx)
        tx2.addInput(tx1.getHash(), 0);

        // try to send 25.0 coins to on two different public keys
        KeyPair keyPair2 = generateNewKeyPair();
        tx2.addOutput(15.0, keyPair2.getPublic());
        KeyPair keyPair3 = generateNewKeyPair();
        tx2.addOutput(10.0, keyPair3.getPublic());

        /*
         * let's sign with our private key to prove the ownership of the
         * first public key specified by the tx1 output
         */
        byte[] sig = sign(keyPair.getPrivate(), tx2.getRawDataToSign(0));
        tx2.addSignature(sig, 0);
        tx2.finalize();

        // TX3 tries to spend the coins it got from Tx2. ( This should return true )
        Transaction tx3 = new Transaction();
        tx3.addInput(tx2.getHash(), 0);

        // Generate new key pair
        KeyPair keyPair4 = generateNewKeyPair();
        // Spend 15 coins
        tx3.addOutput(15.0, keyPair4.getPublic());

        // Sign the transaction
        byte[] signature = sign(keyPair2.getPrivate(), tx3.getRawDataToSign(0));
        tx3.addSignature(signature, 0);
        // Finalize the transaction
        tx3.finalize();

        // TX4 tries to spend more than you have ( This should return false )
        Transaction tx4 = new Transaction();
        tx4.addInput(tx2.getHash(), 1);

        // Generate new two new key pair ( Tx4 owns only 10 coins )
        KeyPair keyPair5 = generateNewKeyPair();
        tx4.addOutput(6.0, keyPair5.getPublic());
        // Try changing this value to <= 4.0 and check if it returns true
        KeyPair keyPair6 = generateNewKeyPair();
        tx4.addOutput(5.0, keyPair6.getPublic());

        // Sign the transaction
        byte[] signature2 = sign(keyPair3.getPrivate(), tx4.getRawDataToSign(0));
        tx4.addSignature(signature2, 0);
        // Finalize the transaction
        tx4.finalize();

        // TX5 uses invalid input hash, same owner of Tx4 ( This should return false )
        Transaction tx5 = new Transaction();
        // Create invalid hash
        byte[] tmp = tx2.getHash().clone();
        tmp[0]++;
        tx5.addInput(tmp, 0);

        // Generate new key pair
        KeyPair keyPair7 = generateNewKeyPair();
        tx5.addOutput(10.0, keyPair7.getPublic());

        // Sign the transaction
        byte[] signature3 = sign(keyPair3.getPrivate(), tx5.getRawDataToSign(0));
        tx5.addSignature(signature3, 0);
        // Finalize the transaction
        tx5.finalize();

        // TX6 this is the same owner of Tx3 tries to double spend
        // ( this should return true if Tx3 is false and vice versa, Only one should go
        // through )
        Transaction tx6 = new Transaction();
        tx6.addInput(tx2.getHash(), 0);

        // Generate new key pair
        KeyPair keyPair8 = generateNewKeyPair();
        tx6.addOutput(15.0, keyPair8.getPublic());

        // Sign the transaction
        byte[] signature4 = sign(keyPair2.getPrivate(), tx6.getRawDataToSign(0));
        tx6.addSignature(signature4, 0);
        // Finalize the transaction
        tx6.finalize();

        // TX7 tries to use same input utxo twice. same owner of Tx3 ( This should
        // return false )
        Transaction tx7 = new Transaction();
        tx7.addInput(tx2.getHash(), 0);
        tx7.addInput(tx2.getHash(), 1);

        // Generate new key pair
        KeyPair keyPair9 = generateNewKeyPair();
        // Spend 25 coins
        tx7.addOutput(25.0, keyPair9.getPublic());

        // Sign the transaction for input 1
        byte[] signature51 = sign(keyPair2.getPrivate(), tx7.getRawDataToSign(0));
        tx7.addSignature(signature51, 0);
        // Sign the transaction for input 2
        byte[] signature52 = sign(keyPair2.getPrivate(), tx7.getRawDataToSign(1));
        tx7.addSignature(signature52, 1);
        // Finalize the transaction
        tx7.finalize();

        // TX8 tries to forge signature. ( This should return false )
        Transaction tx8 = new Transaction();
        tx8.addInput(tx2.getHash(), 1);

        // Generate new key pair
        KeyPair keyPair10 = generateNewKeyPair();
        // Spend 5 coins
        tx8.addOutput(5.0, keyPair10.getPublic());

        // Sign the transaction
        byte[] signature6 = sign(keyPair10.getPrivate(), tx8.getRawDataToSign(0));
        tx8.addSignature(signature6, 0);
        // Finalize the transaction
        tx8.finalize();

        System.out.println("Transaction valid? " + txHandler.isValidTx(tx3));

        // End of transactions, Next try different ordering
        Transaction[] T = { tx2, tx3, tx4, tx5, tx6, tx7, tx8 };
        // Test 1 ( handle all transactions in order (2) valid transactions )
        Test1(new TxHandler(pool), T);
        // Test 2 ( handle all transactions unordered (2) valid transactions )
        Test2(new TxHandler(pool), T.clone());
        // Test 3 ( Check all valid and invalid transactions )
        Test3(new TxHandler(pool), T);

        /*
         * The previous code only checks the validity. To update the
         * pool, your implementation of handleTxs() will be called.
         */

    }

    public static void Test1(TxHandler txHandler, Transaction[] T) {
        int n = txHandler.handleTxs(T).length;
        if (n == 2) {
            System.out.println("Test 1 Passed!");
        } else {
            System.out.println("Test 1 failed: Expected 2 valid transactions Got " + n);
        }
    }

    public static void Test2(TxHandler txHandler, Transaction[] T) {
        Transaction tmp = T[0];
        T[0] = T[1];
        T[1] = tmp;
        int n = txHandler.handleTxs(T).length;
        if (n == 2) {
            System.out.println("Test 2 Passed!");
        } else {
            System.out.println("Test 2 failed: Expected 2 valid transactions Got " + n);
        }
    }

    public static void Test3(TxHandler txHandler, Transaction[] T) {
        // Check if tx2 valid
        if (!txHandler.isValidTx(T[0])) {
            System.out.println("Test 3 Failed!: Tx2 valid, got invalid");
            return;
        }
        // handle Tx2
        Transaction[] tmp = { T[0] };
        txHandler.handleTxs(tmp);
        // Check if Tx3 is valid
        if (!txHandler.isValidTx(T[1])) {
            System.out.println("Test 3 Failed!: Tx3 valid after handling Tx2, got invalid");
            return;
        }
        // Check if Tx4 is valid
        if (txHandler.isValidTx(T[2])) {
            System.out.println("Test 3 Failed!: Tx4 invalid, got valid");
            return;
        }
        // Check if Tx5 is valid
        if (txHandler.isValidTx(T[3])) {
            System.out.println("Test 3 Failed!: Tx5 invalid, got valid");
            return;
        }
        // Check if Tx6 is valid ( Should be valid as Tx3 is not handled yet )
        if (!txHandler.isValidTx(T[4])) {
            System.out.println("Test 3 Failed!: Tx6 valid after handling Tx2, got invalid");
            return;
        }
        // Check if Tx7 is valid ( utxo is in pool but used twice )
        if (txHandler.isValidTx(T[5])) {
            System.out.println("Test 3 Failed!: Tx7 invalid, got valid");
            return;
        }

        // Check if Tx8 is valid ( Forging signature )
        if (txHandler.isValidTx(T[6])) {
            System.out.println("Test 3 Failed!: Tx8 invalid, got valid");
            return;
        }
        System.out.println("Test 3 Passed!");

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