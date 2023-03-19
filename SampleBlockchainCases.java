import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;

public class SampleBlockchainCases {

    public static void main(String[] args) {

        try {
            case1();
            case2();
            case3();
            case4();
            case5();
            case6();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void case1() throws Exception {

        KeyPair keyPair1 = generateNewKeyPair();
        KeyPair keyPair2 = generateNewKeyPair();

        Block genesisBlock = new Block(null, keyPair1.getPublic());
        genesisBlock.finalize();

        BlockChain blockChain = new BlockChain(genesisBlock);
        BlockHandler blockHandler = new BlockHandler(blockChain);

        // This case processes a block with one valid transaction
        Block block = new Block(genesisBlock.getHash(), keyPair1.getPublic());
        // This transaction spends the coinbase transaction in the genesis block
        Transaction tx = new Transaction();
        tx.addInput(genesisBlock.getCoinbase().getHash(), 0);
        tx.addOutput(Block.COINBASE, keyPair2.getPublic());
        tx.addSignature(sign(keyPair1.getPrivate(), tx.getRawDataToSign(0)), 0);
        tx.finalize();

        block.addTransaction(tx);
        block.finalize();

        boolean isSuccessful = blockHandler.processBlock(block);
        if (!isSuccessful) {
            throw new RuntimeException("Unexpected failure");
        }
        System.out.println("Case 1 is OK");
    }

    private static void case2() throws Exception {

        // This case tests the cut-off condition

        Block genesisBlock = new Block(null, generateNewKeyPair().getPublic());
        genesisBlock.finalize();
        BlockChain blockChain = new BlockChain(genesisBlock);
        BlockHandler blockHandler = new BlockHandler(blockChain);

        Block prevBlock = genesisBlock;

        for (int i = 0; i < 20; i++) {
            // This block extends the prevBlock (the block with maxHeight), so it should be
            // added normally.
            Block block = new Block(prevBlock.getHash(), generateNewKeyPair().getPublic());
            block.finalize();
            boolean isSuccessful = blockHandler.processBlock(block);
            if (!isSuccessful) {
                throw new RuntimeException("Unexpected failure");
            }
            prevBlock = block;

            // This block extends the Genesis block, so it should be added normally in the
            // first 10 iterations (assuming Blockchain.CUT_OFF_AGE = 10)
            Block block2 = new Block(genesisBlock.getHash(), generateNewKeyPair().getPublic());
            block2.finalize();
            boolean hasFailed = !blockHandler.processBlock(block2);
            if (i < 10) {
                if (hasFailed) {
                    throw new RuntimeException("Unexpected failure");
                }
            } else {
                if (!hasFailed) {
                    // Note: maxHeight at this point is 12 or more.
                    throw new RuntimeException("Adding a block pointing to Genesis should have failed at this point.");
                }
            }
        }

        System.out.println("Case 2 is OK");
    }

    private static void case3() throws Exception {

        // This case tries creating two blocks, one valid and one invalid
        KeyPair keyPair = generateNewKeyPair();
        Block genesisBlock = new Block(null, keyPair.getPublic());
        genesisBlock.finalize();
        BlockChain blockChain = new BlockChain(genesisBlock);
        BlockHandler blockHandler = new BlockHandler(blockChain);
        // Create tx1 that spends coin base transaction
        Transaction tx1 = new Transaction();
        tx1.addInput(genesisBlock.getCoinbase().getHash(), 0);
        // Create new keyPair to send transaction to
        KeyPair keyPair1 = generateNewKeyPair();
        // specify an output of value 25.0, and the public key
        tx1.addOutput(25.0, keyPair1.getPublic());
        // Sign the transaction
        tx1.addSignature(sign(keyPair.getPrivate(), tx1.getRawDataToSign(0)), 0);
        // needed to compute the id of tx1
        tx1.finalize();

        // tx2 spend tx1 output
        Transaction tx2 = new Transaction();
        // One input of tx2 must refer to the UTXO above (hash, idx)
        tx2.addInput(tx1.getHash(), 0);
        // try to send 25.0 coins to on two different public keys
        KeyPair keyPair2 = generateNewKeyPair();
        tx2.addOutput(15.0, keyPair2.getPublic());
        KeyPair keyPair3 = generateNewKeyPair();
        tx2.addOutput(10.0, keyPair3.getPublic());
        // Sign tx2 inputs
        byte[] sig = sign(keyPair1.getPrivate(), tx2.getRawDataToSign(0));
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

        // block 1 shouldn't be accepted as it contains tx4 which is invalid
        Block block1 = new Block(genesisBlock.getHash(), keyPair.getPublic());
        block1.addTransaction(tx1);
        block1.addTransaction(tx2);
        block1.addTransaction(tx3);
        block1.addTransaction(tx4);
        block1.finalize();
        boolean notFailed = blockHandler.processBlock(block1);
        if (notFailed) {
            throw new RuntimeException("Block contain invalid transaction and got accepted !!!");
        }

        // block 2 should be accepted
        Block block2 = new Block(genesisBlock.getHash(), keyPair.getPublic());
        block2.addTransaction(tx1);
        block2.addTransaction(tx2);
        block2.addTransaction(tx3);
        block2.finalize();
        boolean isSuccessful = blockHandler.processBlock(block2);
        if (!isSuccessful) {
            throw new RuntimeException("Block contain all valid transactions and got rejected !!!");
        }

        System.out.println("Case 3 is OK");
    }

    private static void case4() throws Exception {

        // This case uses block handler and transaction pool to add block
        KeyPair keyPair = generateNewKeyPair();
        Block genesisBlock = new Block(null, keyPair.getPublic());
        genesisBlock.finalize();
        BlockChain blockChain = new BlockChain(genesisBlock);
        BlockHandler blockHandler = new BlockHandler(blockChain);
        // Create tx1 that spends coin base transaction
        Transaction tx1 = new Transaction();
        tx1.addInput(genesisBlock.getCoinbase().getHash(), 0);
        // Create new keyPair to send transaction to
        KeyPair keyPair1 = generateNewKeyPair();
        // specify an output of value 25.0, and the public key
        tx1.addOutput(25.0, keyPair1.getPublic());
        // Sign the transaction
        tx1.addSignature(sign(keyPair.getPrivate(), tx1.getRawDataToSign(0)), 0);
        // needed to compute the id of tx1
        tx1.finalize();

        // tx2 spend tx1 output
        Transaction tx2 = new Transaction();
        // One input of tx2 must refer to the UTXO above (hash, idx)
        tx2.addInput(tx1.getHash(), 0);
        // try to send 25.0 coins to on two different public keys
        KeyPair keyPair2 = generateNewKeyPair();
        tx2.addOutput(15.0, keyPair2.getPublic());
        KeyPair keyPair3 = generateNewKeyPair();
        tx2.addOutput(10.0, keyPair3.getPublic());
        // Sign tx2 inputs
        byte[] sig = sign(keyPair1.getPrivate(), tx2.getRawDataToSign(0));
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

        // Add transactions to transaction pool
        blockHandler.processTx(tx4);
        blockHandler.processTx(tx3);
        blockHandler.processTx(tx1);
        blockHandler.processTx(tx2);
        // Use block handler to add block to max height block
        if (blockHandler.createBlock(keyPair.getPublic()) == null) {
            throw new RuntimeException("Block handler couldn't create block !!!");
        }
        System.out.println("Case 4 is OK");
    }

    private static void case5() throws Exception {

        KeyPair keyPair1 = generateNewKeyPair();
        KeyPair keyPair2 = generateNewKeyPair();

        Block genesisBlock = new Block(null, keyPair1.getPublic());
        genesisBlock.finalize();

        BlockChain blockChain = new BlockChain(genesisBlock);
        BlockHandler blockHandler = new BlockHandler(blockChain);

        // Create new hash value similar to that of genesis block
        byte[] tmp = genesisBlock.getHash().clone();
        tmp[0]++;
        // This case processes a block with one valid transaction
        Block block = new Block(tmp, keyPair1.getPublic());
        // This transaction spends the coinbase transaction in the genesis block
        Transaction tx = new Transaction();
        tx.addInput(genesisBlock.getCoinbase().getHash(), 0);
        tx.addOutput(Block.COINBASE, keyPair2.getPublic());
        tx.addSignature(sign(keyPair1.getPrivate(), tx.getRawDataToSign(0)), 0);
        tx.finalize();

        block.addTransaction(tx);
        block.finalize();

        boolean isSuccessful = blockHandler.processBlock(block);
        if (isSuccessful) {
            throw new RuntimeException("Block contains invalid hash and got accepted !!!");
        }
        System.out.println("Case 5 is OK");
    }

    private static void case6() throws Exception {

        // This case tests memory leak

        Block genesisBlock = new Block(null, generateNewKeyPair().getPublic());
        genesisBlock.finalize();
        BlockChain blockChain = new BlockChain(genesisBlock);
        BlockHandler blockHandler = new BlockHandler(blockChain);

        Block prevBlock = genesisBlock;

        for (int i = 0; i < 10000; i++) {
            // This block extends the prevBlock (the block with maxHeight), so it should be
            // added normally.
            Block block = new Block(prevBlock.getHash(), generateNewKeyPair().getPublic());
            block.finalize();
            boolean isSuccessful = blockHandler.processBlock(block);
            if (!isSuccessful) {
                throw new RuntimeException("Couldn't add valid block to max height block");
            }
            prevBlock = block;
        }

        System.out.println("Case 6 is OK");
    }

    private static KeyPair generateNewKeyPair() throws NoSuchAlgorithmException, NoSuchProviderException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(1024); // Warning: This is a small value for testing. 1024-bit RSA keys do not provide
                                 // the recommended security level.
        return keyGen.genKeyPair();
    }

    private static byte[] sign(PrivateKey privKey, byte[] message)
            throws NoSuchAlgorithmException, SignatureException,
            InvalidKeyException {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privKey);
        signature.update(message);
        return signature.sign();
    }
}
