import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.*;

import org.web3j.abi.*;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Uint256;

import org.web3j.utils.Numeric;

import org.web3j.crypto.*;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.protocol.http.HttpService;
import org.web3j.protocol.core.DefaultBlockParameterNumber;


public class Demo4ERC20 {

    public static String WEB3_RPC_URI = "https://rpc1.timechain.gold";
    public static Web3j web3j = Web3j.build(new HttpService(WEB3_RPC_URI));
    public static String CONTRACT_ADDRESS = "0x9ef53b649bfee68f3d2c5e38e0768438460d160a";

    // The topic corresponding to the transfer method of token
    public static String TRANSFER_TOPICS0 = "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef";
    public static int decimals = 18;

    public static void main(String[] args) {
        try {
            System.out.println("\n======start======\n");

            String address = "0x5F6af52690818502cf54cC9f0A4782c8c0d8aef3";
            BigDecimal amountWei = getTokenBalance(address, CONTRACT_ADDRESS);

            BigDecimal amount = amountWei.divide(BigDecimal.TEN.pow(decimals));
            System.out.printf("#address: %s\n", address);
            System.out.printf("#amountWei: %s\n", amountWei);
            System.out.printf("#amountToken: %s\n", amount.setScale(6, RoundingMode.CEILING));

//            offlineSendERC20WithGas();

            System.out.println("\n============");
            BigInteger blocknumber = web3j.ethBlockNumber().send().getBlockNumber();
            System.out.printf("#blocknumber: %s\n", blocknumber);

            BigInteger start = BigInteger.valueOf(16117);
            //  BigInteger end = blocknumber.subtract(BigInteger.valueOf(5));
            BigInteger end = BigInteger.valueOf(16150);

            // start to scan the TOKEN transfer record from the specified block height,
            // and when scan to the height of the latest height - 5,
            // it is recommended to sleep for 10 seconds and continue scanning.
            // So after each scan, you need to record the height of the block at the end of this scan.
//            scanTokenTransaction(start, end);

            System.out.println("\n======end###\n");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static BigDecimal getTokenBalance(String myAddress, String contractAddress) {
        try {
            List<TypeReference<?>> outputParameters = new ArrayList<>();
            outputParameters.add(new TypeReference<Uint256>() {
            });
            Function function = new Function(
                    "balanceOf",
                    Collections.singletonList(new Address(myAddress)),
                    outputParameters
            );
            String encodedFunction = FunctionEncoder.encode(function);
            org.web3j.protocol.core.methods.response.EthCall response = web3j.ethCall(
                    Transaction.createEthCallTransaction(myAddress, contractAddress, encodedFunction),
                    DefaultBlockParameterName.LATEST)
                    .sendAsync().get();

            List<Type> someTypes = FunctionReturnDecoder.decode(
                    response.getValue(), function.getOutputParameters());

            return new BigDecimal(((Uint256) someTypes.get(0)).getValue());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return BigDecimal.ZERO;
    }


    public static void offlineSendERC20WithGas() {
        try {
            // private key and address for test
            // index, private key, address
            // 0,9afc279d108b1d3686e4bb92d8a0a201f4ddf26e32978cff8b500c1c02ae1ecb,0xddf69b85290f09e93fc264fd0a8b79a945d296e9
            String fromAddress = Keys.toChecksumAddress("0xddf69b85290f09e93fc264fd0a8b79a945d296e9");
            String privateKey = "9afc279d108b1d3686e4bb92d8a0a201f4ddf26e32978cff8b500c1c02ae1ecb";
            String toAddress = Keys.toChecksumAddress("0x5F6af52690818502cf54cC9f0A4782c8c0d8aef3");
            BigDecimal amount = new BigDecimal("10.24");

            // Nonce will +1 each time, so you need to get it in real time
            BigInteger nonce = web3j.ethGetTransactionCount(fromAddress, DefaultBlockParameterName.PENDING).send().getTransactionCount();
            System.out.println("#nonce ：" + nonce);

            // Get real-time gas price
            BigInteger gasPrice = web3j.ethGasPrice().send().getGasPrice();
            System.out.println("#gasPrice ：" + gasPrice);

            // Number of TOKEN transferred Unit conversion, the smallest unit is WEI, 1 TOKEN=10^decimals WEI
            BigInteger amountWei = BigDecimal.TEN.pow(decimals).multiply(amount).toBigInteger();
            Function function = new Function(
                    "transfer",
                    Arrays.asList(new Address(toAddress), new Uint256(amountWei)),
                    Collections.singletonList(new TypeReference<Type>() {
                    }));

            //Create RawTransaction
            String data = FunctionEncoder.encode(function);

            // ERC20 Token gasLimit
            BigInteger gasLimit = BigInteger.ZERO;
            EthEstimateGas ethEstimateGas = web3j.ethEstimateGas(
                    Transaction.createFunctionCallTransaction(fromAddress, nonce, gasPrice, gasLimit, CONTRACT_ADDRESS, data)
            ).send();
            // gasLimit + 10000, in case not enough
            gasLimit = ethEstimateGas.getAmountUsed().add(BigInteger.TEN.pow(4));
            System.out.println("#gasLimit ：" + gasLimit);

            RawTransaction rawTransaction = RawTransaction.createTransaction(
                    nonce, gasPrice, gasLimit, CONTRACT_ADDRESS, data);

            // Sign with privateKey
            Credentials credentials = Credentials.create(privateKey);
            byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
            String hexValue = Numeric.toHexString(signedMessage);

            // Send transaction
            EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(hexValue).send();
            String txHash = ethSendTransaction.getTransactionHash();

            // get transactionHash
            System.out.println("tx:" + txHash);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void scanTokenTransaction(BigInteger startNumber, BigInteger endNumber) {
        try {
            System.out.println("blockNumber,timestamp,txid,from,to,amount");

            for (BigInteger i = startNumber; i.compareTo(endNumber) < 0; i = i.add(BigInteger.valueOf(1))) {
                EthBlock ethBlock = web3j.ethGetBlockByNumber(new DefaultBlockParameterNumber(i), true).send();
                List<EthBlock.TransactionResult> allTransactions = ethBlock.getBlock().getTransactions();
                BigInteger timestamp = ethBlock.getBlock().getTimestamp();
                // The txid needs to be stored in the database,
                // and the status needs to be marked after the recharge successful in order to prevent repeated recharge
                allTransactions.forEach(item -> {
                    EthBlock.TransactionObject txObj = (EthBlock.TransactionObject) item.get();
                    String input = txObj.getInput();
                    // The input field is not 0x,and value=0, it is contract transaction
                    if (input.length() > 10 && txObj.getValue().equals(BigInteger.ZERO)) {
                        try {
                            TransactionReceipt txReceipt = web3j.ethGetTransactionReceipt(txObj.getHash()).send().getResult();
                            List<Log> txLogs = txReceipt.getLogs();
                            // The contract is valid only if it is executed successfully
                            if (txReceipt.isStatusOK() && txLogs.size() > 0) {
                                // There is only one transfer events for standard ERC20 TOKEN
                                Log txLog = txLogs.get(0);
                                List<String> topics = txLog.getTopics();
                                String topics0 = topics.get(0);
                                // Verify the contract address, in case other tokens to recharge
                                // Verify topics0, only process transfer method
                                if (CONTRACT_ADDRESS.equalsIgnoreCase(txLog.getAddress())
                                    && topics0.equalsIgnoreCase(TRANSFER_TOPICS0) ) {
                                    String fromAddress = Numeric.prependHexPrefix(topics.get(1).substring(26, 66));
                                    String toAddress =  Numeric.prependHexPrefix(topics.get(2).substring(26, 66));
                                    String data = txLog.getData();
                                    BigDecimal amountWei = new BigDecimal(Numeric.toBigInt(data));
                                    BigDecimal amount = amountWei.divide(BigDecimal.TEN.pow(decimals)).setScale(6, RoundingMode.CEILING);
                                    ;
                                    System.out.println(txLog.getBlockNumber()
                                            + "," + timestamp
                                            + "," + txLog.getTransactionHash()
                                            + "," + fromAddress
                                            + "," + toAddress
                                            + "," + amount
                                            );
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
