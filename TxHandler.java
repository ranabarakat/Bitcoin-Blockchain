/* 
 * I acknowledge that I am aware of the academic integrity guidelines of this course, 
 * and that I worked on this assignment independently without any unauthorized help with 
 * coding or testing. - Rana Mohamed Barakat 
 */

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;

public class TxHandler {

    private UTXOPool utxoPool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent
     * transaction outputs) is
     * {@code utxoPool}.
     */
    public TxHandler(UTXOPool utxoPool) {
        this.utxoPool = new UTXOPool(utxoPool);
    }

    public UTXOPool getUTXOPool() {
        return this.utxoPool;
    }

    /**
     * @return true if:
     *         (1) all outputs claimed by {@code tx} are in the current UTXO pool,
     *         (2) the signatures on each input of {@code tx} are valid,
     *         (3) no UTXO is claimed multiple times by {@code tx},
     *         (4) all of {@code tx}s output values are non-negative, and
     *         (5) the sum of {@code tx}s input values is greater than or equal to
     *         the sum of its output
     *         values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        // IMPLEMENT THIS

        // get all inputs in tx
        ArrayList<Transaction.Input> in = tx.getInputs();

        // pool to track if utxo is claimed multiple times
        UTXOPool UTXOTrack = new UTXOPool();

        // track sum of input and output values
        double inputSum = 0;
        double outputSum = 0;

        for (int i = 0; i < in.size(); i++) {
            // validate (1)
            UTXO utxo = new UTXO(in.get(i).prevTxHash, in.get(i).outputIndex);
            if (!this.utxoPool.contains(utxo)) {
                return false;
            }

            // validate (2)
            PublicKey pubKey = this.utxoPool.getTxOutput(utxo).address;
            byte[] rawData = tx.getRawDataToSign(i);
            if (!Crypto.verifySignature(pubKey, rawData, in.get(i).signature)) {
                return false;
            }

            // validate (3)
            if (UTXOTrack.contains(utxo)) {
                return false;
            }
            UTXOTrack.addUTXO(utxo, this.utxoPool.getTxOutput(utxo));

            inputSum += utxoPool.getTxOutput(utxo).value;

        }
        // validate (4)
        ArrayList<Transaction.Output> out = tx.getOutputs();
        for (int i = 0; i < out.size(); i++) {
            if (out.get(i).value < 0.0) {
                return false;
            }
            outputSum += out.get(i).value;
        }

        // validate (5)
        if (inputSum < outputSum) {
            return false;
        }

        return true;

    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions,
     * checking each
     * transaction for correctness, returning a mutually valid array of accepted
     * transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS

        ArrayList<Transaction> validTxs = new ArrayList<>();
        ArrayList<Transaction> temp = new ArrayList<>();
        ArrayList<Transaction> cloneTxs = new ArrayList<>(Arrays.asList(possibleTxs));
        boolean isModified;
        do {
            isModified = false;
            for (Transaction tx : cloneTxs) {

                if (isValidTx(tx)) {
                    // UTXO is getting updated, giving another chance for prev invalid transactions
                    isModified = true;
                    // Add the valid transaction's outputs the UTXO pool
                    ArrayList<Transaction.Output> out = tx.getOutputs();
                    for (int i = 0; i < out.size(); i++) {
                        this.utxoPool.addUTXO(new UTXO(tx.getHash(), i), tx.getOutput(i));
                    }
                    // Add transaction to the valid list
                    validTxs.add(tx);

                    // Remove UTXO spent by the valid transaction to prevent double spending
                    ArrayList<Transaction.Input> in = tx.getInputs();
                    for (int i = 0; i < in.size(); i++) {
                        UTXO utxo = new UTXO(in.get(i).prevTxHash, in.get(i).outputIndex);
                        this.utxoPool.removeUTXO(utxo);
                        // tempPool.addUTXO(new UTXO(tx.getHash(), i), tx.getOutput(i));
                    }

                    // collect all valid txs to be removed after iterating
                    temp.add(tx);
                }

            }
            for (Transaction tx : temp) {
                cloneTxs.remove(tx);
            }

        } while (isModified && cloneTxs.size() != 0);

        Transaction[] validTransactions = validTxs.toArray(new Transaction[validTxs.size()]);
        return validTransactions;
    }

}
