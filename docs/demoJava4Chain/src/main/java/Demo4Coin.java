import org.bitcoinj.crypto.*;
import org.web3j.crypto.*;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.DefaultBlockParameterNumber;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthGasPrice;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import static java.util.Arrays.asList;


public class Demo4Coin {

    public static String WEB3_RPC_URI = "https://rpc1.timechain.gold";
    public static Web3j web3j = Web3j.build(new HttpService(WEB3_RPC_URI));

    public static void main(String[] args) {
        try {
            System.out.println("\n======start======\n");

//            generateNewAddress();

            String address = "0x20cbc0a9565fba3ab81e8fb2faf1f6c4bf2375e3";
            getBlance(address);

//            offlineSendWithGas();

            BigInteger blocknumber = web3j.ethBlockNumber().send().getBlockNumber();
            System.out.printf("#blocknumber: %s\n", blocknumber);

            BigInteger start = BigInteger.valueOf(7630);
            //   BigInteger end = blocknumber.subtract(BigInteger.valueOf(5));
            BigInteger end = BigInteger.valueOf(7650);

            // start to scan the BASIC COIN transfer record from the specified block height,
            // and when scan to the height of the latest height - 5,
            // it is recommended to sleep for 10 seconds and continue scanning.
            // So after each scan, you need to record the height of the block at the end of this scan.
//            scanAllTransaction(start, end);

            System.out.println("\n======end###\n");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void generateNewAddress() {
        try {
//            SecureRandom secureRandom = new SecureRandom();
//            byte[] entropy = new byte[DeterministicSeed.DEFAULT_SEED_ENTROPY_BITS / 8];
//            secureRandom.engineNextBytes(entropy);
//            // Randomly generate mnemonic words
//            List<String>  words_list = MnemonicCode.INSTANCE.toMnemonic(entropy);

            // Generate address based on specified mnemonic
            String words = "track mandate pool ignore hollow loop amateur sail dog inner pistol spell";
            List<String> words_list = asList(words.split(" "));
            //          System.out.println("mnemonic words：" + words);

            // The BIP44 path prefix used by the ETH is m/44'/60'/0'/0/{i}, the last digit incremented
            List<ChildNumber> ethBip44PathPrefix =
                    asList(new ChildNumber(44, true),
                            new ChildNumber(60, true),
                            ChildNumber.ZERO_HARDENED,
                            ChildNumber.ZERO);

            // generate wallet seeds, mainstream wallet passwords are empty by default
            byte[] seed = MnemonicCode.toSeed(words_list, "");
            DeterministicKey masterPrivateKey = HDKeyDerivation.createMasterPrivateKey(seed);
            DeterministicHierarchy deterministicHierarchy = new DeterministicHierarchy(masterPrivateKey);

            System.out.println("index,private,address");
            System.out.println("===============");

            int addressCount = 10;
            for (int i = 0; i < addressCount; i++) {

                DeterministicKey deterministicKey = deterministicHierarchy
                        .deriveChild(ethBip44PathPrefix, false, true, new ChildNumber(i));
                byte[] bytes = deterministicKey.getPrivKeyBytes();

                ECKeyPair keyPair = ECKeyPair.create(bytes);
                String privateKey = keyPair.getPrivateKey().toString(16);
                // Generate wallet address by public key
                String address = Keys.getAddress(keyPair.getPublicKey());
                // use the checksum address formatting address
                address = Keys.toChecksumAddress(address);
                System.out.println(i + "," + privateKey + "," + address);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void getBlance(String address) {
        try {
            // The second parameter: select the latest block
            EthGetBalance balance = web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST).send();
            // Unit conversion WEI to Ether
            BigDecimal blanceETH = Convert.fromWei(balance.getBalance().toString(), Convert.Unit.ETHER);
            System.out.printf("#address: %s\n", address);
            System.out.printf("#blanceETH: %s\n", blanceETH);

            BigInteger nonce = web3j.ethGetTransactionCount(address, DefaultBlockParameterName.PENDING).send().getTransactionCount();
            System.out.println("#nonce ：" + nonce);

            EthGasPrice ethGasPrice = web3j.ethGasPrice().send();
            BigInteger gasPrice = ethGasPrice.getGasPrice();
            System.out.println("#gasPrice ：" + gasPrice);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void offlineSendWithGas() {
        try {
            // private key and address for test
            // index, private key, address
            // 0,9afc279d108b1d3686e4bb92d8a0a201f4ddf26e32978cff8b500c1c02ae1ecb,0xddf69b85290f09e93fc264fd0a8b79a945d296e9
            String fromAddress = Keys.toChecksumAddress("0xddf69b85290f09e93fc264fd0a8b79a945d296e9");
            String privateKey = "9afc279d108b1d3686e4bb92d8a0a201f4ddf26e32978cff8b500c1c02ae1ecb";
            String toAddress = Keys.toChecksumAddress("0x5F6af52690818502cf54cC9f0A4782c8c0d8aef3");
            String amount = "11.23";

            // Nonce will +1 each time, so you need to get it in real time
            BigInteger nonce = web3j.ethGetTransactionCount(fromAddress, DefaultBlockParameterName.PENDING).send().getTransactionCount();
            System.out.println("#nonce ：" + nonce);
            // transfer ETH, fixed gas
            BigInteger gasLimit = BigInteger.valueOf(21000);
            // Get real-time gas price
            BigInteger gasPrice = web3j.ethGasPrice().send().getGasPrice();
            System.out.println("#gasPrice ：" + gasPrice);

            // Number of coins transferred Unit conversion, the smallest unit is WEI, 1ETH=10^18 WEI
            BigInteger amountWei = Convert.toWei(amount, Convert.Unit.ETHER).toBigInteger();
            RawTransaction rawTransaction = RawTransaction.createEtherTransaction(nonce, gasPrice, gasLimit, toAddress, amountWei);

            // Sign with privateKey
            Credentials credentials = Credentials.create(privateKey);
            byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
            String hexValue = Numeric.toHexString(signedMessage);

            // Send transaction
            EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(hexValue).send();
            String transactionHash = ethSendTransaction.getTransactionHash();

            // get transactionHash
            System.out.println("tx:" + transactionHash);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void scanAllTransaction(BigInteger startNumber, BigInteger endNumber) {
        try {
            System.out.println("\nblockNumber,timestamp,txid,from,to,amount");

            for (BigInteger i = startNumber; i.compareTo(endNumber) < 0; i = i.add(BigInteger.valueOf(1))) {
                EthBlock ethBlock = web3j.ethGetBlockByNumber(new DefaultBlockParameterNumber(i), true).send();
                List<EthBlock.TransactionResult> allTransactions = ethBlock.getBlock().getTransactions();
                BigInteger timestamp = ethBlock.getBlock().getTimestamp();
                // The txid needs to be stored in the database,
                // and the status needs to be marked after the recharge successful in order to prevent repeated recharge
                allTransactions.forEach(item -> {
                    EthBlock.TransactionObject txObj = (EthBlock.TransactionObject) item.get();
                    String input = txObj.getInput();
                    // The input field is 0x, which is considered to be an ETH transaction,
                    // and the token transaction is not empty
                    if (input.equals("0x")) {
                        BigDecimal amount = Convert.fromWei(String.valueOf(txObj.getValue()), Convert.Unit.ETHER);
                        System.out.println(txObj.getBlockNumber()
                                + "," + timestamp
                                + "," + txObj.getHash()
                                + "," + txObj.getFrom()
                                + "," + txObj.getTo()
                                + "," + amount.toString());
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
