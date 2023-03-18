import java.security.PublicKey;
import java.util.ArrayList;

public class TxHandler {

    private UTXOPool utxoPool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. 
     */
    public TxHandler(UTXOPool utxoPool) {
        this.utxoPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
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

        for (int i = 0; i< in.size(); i++){
            // validate (1)
            UTXO utxo = new UTXO(in.get(i).prevTxHash,in.get(i).outputIndex);
            if(!this.utxoPool.contains(utxo)){
                return false;
            }

            // validate (2)
            PublicKey pubKey = this.utxoPool.getTxOutput(utxo).address;
            byte [] rawData = tx.getRawDataToSign(i);
            if(!Crypto.verifySignature(pubKey, rawData, in.get(i).signature)){
                return false;
            }

            // validate (3)
            if(UTXOTrack.contains(utxo)){
                return false;
            }
            UTXOTrack.addUTXO(utxo,this.utxoPool.getTxOutput(utxo));

            inputSum += utxoPool.getTxOutput(utxo).value;
            
        }
        // validate (4)
        ArrayList<Transaction.Output> out = tx.getOutputs();
        for(int i = 0;i<out.size();i++){
            if(out.get(i).value<0.0){
                return false;
            }
            outputSum += out.get(i).value;
        }

        // validate (5)
        if(inputSum<outputSum){
            return false;
        }

        return true;
        
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
    }

}
