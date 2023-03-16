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