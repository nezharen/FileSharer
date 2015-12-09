package space.nezharen.filesharer;

public final class Constants {
    public static final int SERVER_PORT = 9301;

    public static final int SERVER_THREAD_STATUS_LISTENING = 100;
    public static final int SERVER_THREAD_STATUS_WIFI_NOT_CONNECTED = 200;
    public static final int SERVER_THREAD_STATUS_INIT_FAILED = 201;

    public static final int SERVER_STATUS_CONNECTED = 100;
    public static final int SERVER_STATUS_FILE_WAIT_FOR_ACCEPT = 102;
    public static final int SERVER_STATUS_FILE_ACCEPTED = 103;
    public static final int SERVER_STATUS_FILE_REJECTED = 104;
    public static final int SERVER_STATUS_CONNECT_FAILED = 200;

    public static final int CLIENT_STATUS_CONNECTING = 100;
    public static final int CLIENT_STATUS_CONNECTED = 101;
    public static final int CLIENT_STATUS_SENDING_FILE = 102;
    public static final int CLIENT_STATUS_SEND_FILE_DONE = 103;
    public static final int CLIENT_STATUS_RECEIVING_FILE = 104;
    public static final int CLIENT_STATUS_RECEIVE_FILE_DONE = 105;
    public static final int CLIENT_STATUS_CONNECT_FAILED = 200;
    public static final int CLIENT_STATUS_SEND_FILE_FAILED = 201;
    public static final int CLIENT_STATUS_RECEIVE_FILE_FAILED = 202;

    public static final int CLIENT_COMMAND_SEND_FILE = 100;

    public static final int SERVER_COMMAND_ACCEPT_FILE = 101;
    public static final int SERVER_COMMAND_REJECT_FILE = 102;
    public static final int SERVER_COMMAND_RECEIVE_FILE_SUCCESS = 103;
    public static final int SERVER_COMMAND_RECEIVE_FILE_FAILED = 104;

    public static final int SERVERS_STATUS_FILE_WAIT_FOR_ACCEPT = 100;

    public static final int CLIENTS_STATUS_UPDATE_STATUS = 100;
    public static final int CLIENTS_STATUS_RECEIVE_FILE_SUCCESS = 101;
    public static final int CLIENTS_STATUS_RECEIVE_FILE_FAILED = 102;

    public static final String SERVER_THREAD_STATUS = "SERVER_THREAD_STATUS";
    public static final String SERVERS_STATUS = "SERVERS_STATUS";
    public static final String CLIENTS_STATUS = "CLIENTS_STATUS";
    public static final String SERVER_HOST = "SERVER_HOST";
    public static final String SERVER_FILENAME = "SERVER_FILENAME";
    public static final String RECEIVED_FILE_PATH = "RECEIVED_FILE_PATH";
    public static final String RECEIVED_FILE_NAME = "RECEIVED_FILE_NAME";
    public static final String IP_ADDRESS = "IP_ADDRESS";

    public static final int SERVER_NOTIFICATION_ID = 0;
    public static final int SELECT_FILE_TO_SEND_CODE = 100;
    public static final int BUFFER_SIZE = 8192;
}
