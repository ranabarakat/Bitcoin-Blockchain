/* 
 * I acknowledge that I am aware of the academic integrity guidelines of this course, 
 * and that I worked on this assignment independently without any unauthorized help with 
 * coding or testing. - Rana Mohamed Barakat 
 */

// The BlockChain class should maintain only limited block nodes to satisfy the functionality.
// You should not have all the blocks added to the block chain in memory 
// as it would cause a memory overflow.
// import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;

public class BlockChain {
    public static final int CUT_OFF_AGE = 10;
    private int maxHeight;
    private Node maxHeightNode;
    TransactionPool transactionPool = new TransactionPool();
    HashMap<ByteArrayWrapper, Node> blockchain = new HashMap<ByteArrayWrapper, Node>();

    // UTXOPool utxoPool = new UTXOPool();

    private class Node {
        int height;
        Node parent;
        ArrayList<Node> children = new ArrayList<Node>();
        UTXOPool utxoPool = new UTXOPool();
        Block block;

        public Node(UTXOPool pool, Node parent, Block block) {
            this.utxoPool = pool;
            this.parent = parent;
            this.block = block;
            if (!(parent == null)) {
                this.parent.children.add(this);
                this.height = this.parent.height + 1;
            } else {
                this.height = 1;
            }
        }
    }

    /**
     * create an empty blockchain with just a genesis block. Assume
     * {@code genesisBlock} is a valid
     * block
     */
    public BlockChain(Block genesisBlock) {
        // IMPLEMENT THIS

        this.maxHeight = 1;
        UTXOPool utxoPool = new UTXOPool();
        ByteArrayWrapper hash = new ByteArrayWrapper(genesisBlock.getHash());
        Node node = new Node(utxoPool, null, genesisBlock);
        blockchain.put(hash, node);
        this.maxHeightNode = node;

    }

    /** Get the maximum height block */
    public Block getMaxHeightBlock() {
        // IMPLEMENT THIS
        return this.maxHeightNode.block;
    }

    /** Get the UTXOPool for mining a new block on top of max height block */
    public UTXOPool getMaxHeightUTXOPool() {
        // IMPLEMENT THIS
        return this.maxHeightNode.utxoPool;

    }

    /** Get the transaction pool to mine a new block */
    public TransactionPool getTransactionPool() {
        // IMPLEMENT THIS
        return this.transactionPool;

    }

    /**
     * Add {@code block} to the blockchain if it is valid. For validity, all
     * transactions should be
     * valid and block should be at {@code height > (maxHeight - CUT_OFF_AGE)},
     * where maxHeight is
     * the current height of the blockchain.
     * <p>
     * Assume the Genesis block is at height 1.
     * For example, you can try creating a new block over the genesis block (i.e.
     * create a block at
     * height 2) if the current blockchain height is less than or equal to
     * CUT_OFF_AGE + 1. As soon as
     * the current blockchain height exceeds CUT_OFF_AGE + 1, you cannot create a
     * new block at height 2.
     * 
     * @return true if block is successfully added
     */
    public boolean addBlock(Block block) {
        // IMPLEMENT THIS
        if (block == null) {
            return false;
        }

        Node parentNode = blockchain.get(new ByteArrayWrapper(block.getPrevBlockHash()));

        // new block cannot be a genesis block
        if (parentNode == null) {
            return false;
        }

        // check that all transactions in a block are valid together
        TxHandler txHandler = new TxHandler(parentNode.utxoPool);
        Transaction[] blockTxs = block.getTransactions().toArray(new Transaction[block.getTransactions().size()]);
        // If they are not the same size, some transactions were invalid
        // if (txHandler.handleTxs(blockTxs).length != blockTxs.length) {
        // return false;
        // }

        if (!Arrays.equals(txHandler.handleTxs(blockTxs), blockTxs)) {
            return false;
        }

        // new block should be at height > (maxHeight - CUT_OFF_AGE)
        if (parentNode.height + 1 <= this.maxHeight - CUT_OFF_AGE) {
            return false;
        }

        // add block to blockchain
        UTXOPool newPool = new UTXOPool();
        newPool = txHandler.getUTXOPool();
        Node newNode = new Node(newPool, parentNode, block);
        blockchain.put(new ByteArrayWrapper(block.getHash()), newNode);

        // remove block txs from transaction pool after adding them to the blockchain
        for (Transaction tx : blockTxs) {
            this.transactionPool.removeTransaction(tx.getHash());
        }
        if (newNode.height > this.maxHeight) {
            // update maxHeight and maxHeightNode if needed
            this.maxHeight = newNode.height;
            this.maxHeightNode = newNode;
        }

        // delete old blocks
        Iterator<Map.Entry<ByteArrayWrapper, Node>> it = blockchain.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<ByteArrayWrapper, Node> entry = it.next();

            if (entry.getValue().height < this.maxHeight - CUT_OFF_AGE) {
                it.remove();
            }
        }

        return true;

    }

    /** Add a transaction to the transaction pool */
    public void addTransaction(Transaction tx) {
        // IMPLEMENT THIS
        this.transactionPool.addTransaction(tx);
    }
}