package space.nezharen.filesharer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        servers = new ArrayList<Server>();
        clients = new ArrayList<Client>();

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.activity_main_toolbar);
        setSupportActionBar(toolbar);

        RecyclerView clientsRecyclerView = (RecyclerView) findViewById(R.id.clients_recycler_view);
        clientsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        clientsAdapter = new ClientsAdapter();
        clientsRecyclerView.setAdapter(clientsAdapter);
        clientsRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST));

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.activity_main_fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder aBuilder = new AlertDialog.Builder(MainActivity.this);
                aBuilder.setIcon(R.mipmap.ic_launcher);
                aBuilder.setTitle(getString(R.string.app_name));
                LayoutInflater inflater = MainActivity.this.getLayoutInflater();
                final View dialog_add_view = inflater.inflate(R.layout.dialog_add, null);
                aBuilder.setView(dialog_add_view);
                aBuilder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        EditText ipEditText = (EditText) dialog_add_view.findViewById(R.id.ip_address_edit_text);
                        String hostAddress = ipEditText.getText().toString().trim();
                        if (!(hostAddress.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}"))) {
                            Toast.makeText(getApplicationContext(), getString(R.string.invalid_ip_address), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        boolean inClients = false;
                        for (Client client:clients)
                            if (client.host.equals(hostAddress)) {
                                inClients = true;
                                break;
                            }
                        if (!inClients) {
                            Client newClient = new Client(hostAddress);
                            clients.add(newClient);
                            newClient.connect();
                        }
                        else {
                            Toast.makeText(getApplicationContext(), getString(R.string.client_already_added), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                aBuilder.setNegativeButton(getString(R.string.cancel), null);
                aBuilder.setCancelable(false);
                aBuilder.show();
            }
        });

        serverMessageHandler = new ServerMessageHandler();
        clientMessageHandler = new ClientMessageHandler();

        serverThreadMessageHandler = new ServerThreadMessageHandler();
        serverThread = new ServerThread();
        serverThread.start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
        System.exit(0);
    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder aBuilder = new AlertDialog.Builder(this);
        aBuilder.setIcon(R.mipmap.ic_launcher);
        aBuilder.setTitle(getString(R.string.app_name));
        aBuilder.setMessage(getString(R.string.quit_confirm));
        aBuilder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        aBuilder.setNegativeButton(getString(R.string.cancel), null);
        aBuilder.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_help) {
            Intent intent = new Intent(this, HelpActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case Constants.SELECT_FILE_TO_SEND_CODE:
                if (resultCode == RESULT_OK) {
                    Uri uri = data.getData();
                    String path = GetPathFromUri4kitkat.getPath(this, uri);
                    clients.get(currentClient).sendFile(path);
                }
                break;
        }
    }

    public class ClientsAdapter extends RecyclerView.Adapter<ClientsAdapter.ClientsViewHolder> {
        @Override
        public ClientsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_main, parent, false);
            return new ClientsViewHolder(v);
        }

        @Override
        public void onBindViewHolder(final ClientsViewHolder holder, int position) {
            holder.clientHostTextView.setText(clients.get(position).host);
            switch (clients.get(position).status) {
                case Constants.CLIENT_STATUS_CONNECTING:
                    holder.clientStatusTextView.setText(getString(R.string.connecting));
                    break;
                case Constants.CLIENT_STATUS_CONNECTED:
                    holder.clientStatusTextView.setText(getString(R.string.connected));
                    break;
                case Constants.CLIENT_STATUS_CONNECT_FAILED:
                    holder.clientStatusTextView.setText(getString(R.string.connect_failed));
                    break;
                case Constants.CLIENT_STATUS_SENDING_FILE:
                    holder.clientStatusTextView.setText(getString(R.string.sending_file));
                    break;
                case Constants.CLIENT_STATUS_SEND_FILE_DONE:
                    holder.clientStatusTextView.setText(getString(R.string.send_file_done));
                    break;
                case Constants.CLIENT_STATUS_SEND_FILE_FAILED:
                    holder.clientStatusTextView.setText(getString(R.string.send_file_failed));
                    break;
                case Constants.CLIENT_STATUS_RECEIVING_FILE:
                    holder.clientStatusTextView.setText(getString(R.string.receiving_file));
                    break;
                case Constants.CLIENT_STATUS_RECEIVE_FILE_DONE:
                    holder.clientStatusTextView.setText(getString(R.string.receive_file_done));
                    break;
                case Constants.CLIENT_STATUS_RECEIVE_FILE_FAILED:
                    holder.clientStatusTextView.setText(getString(R.string.receive_file_failed));
                    break;
                default:
                    holder.clientStatusTextView.setText(getString(R.string.unknown_error));
                    break;
            }
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    currentClient = holder.getLayoutPosition();
                    int status = clients.get(currentClient).status;
                    if (status == Constants.CLIENT_STATUS_CONNECTED || status == Constants.CLIENT_STATUS_RECEIVE_FILE_DONE || status == Constants.CLIENT_STATUS_SEND_FILE_DONE ||
                            status == Constants.CLIENT_STATUS_RECEIVE_FILE_FAILED || status == Constants.CLIENT_STATUS_SEND_FILE_FAILED) {
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("*/*");
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        try {
                            startActivityForResult(Intent.createChooser(intent, getString(R.string.select_file_to_send)), Constants.SELECT_FILE_TO_SEND_CODE);
                        } catch (Exception e) {
                            Toast.makeText(getApplicationContext(), getString(R.string.file_manager_not_found), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return clients.size();
        }

        public class ClientsViewHolder extends RecyclerView.ViewHolder {
            public ClientsViewHolder(View view) {
                super(view);
                clientHostTextView = (TextView) view.findViewById(R.id.client_host);
                clientStatusTextView = (TextView) view.findViewById(R.id.client_status);
            }

            public TextView clientHostTextView, clientStatusTextView;
        }
    }

    public class DividerItemDecoration extends RecyclerView.ItemDecoration {

        private final int[] ATTRS = new int[]{
                android.R.attr.listDivider
        };

        public static final int HORIZONTAL_LIST = LinearLayoutManager.HORIZONTAL;

        public static final int VERTICAL_LIST = LinearLayoutManager.VERTICAL;

        private Drawable mDivider;

        private int mOrientation;

        public DividerItemDecoration(Context context, int orientation) {
            final TypedArray a = context.obtainStyledAttributes(ATTRS);
            mDivider = a.getDrawable(0);
            a.recycle();
            setOrientation(orientation);
        }

        public void setOrientation(int orientation) {
            if (orientation != HORIZONTAL_LIST && orientation != VERTICAL_LIST) {
                throw new IllegalArgumentException("invalid orientation");
            }
            mOrientation = orientation;
        }

        @Override
        public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {

            if (mOrientation == VERTICAL_LIST) {
                drawVertical(c, parent, state);
            } else {
                drawHorizontal(c, parent, state);
            }

        }

        public void drawVertical(Canvas c, RecyclerView parent, RecyclerView.State state) {
            final int left = parent.getPaddingLeft();
            final int right = parent.getWidth() - parent.getPaddingRight();

            final int childCount = parent.getChildCount();
            for (int i = 0; i < childCount; i++) {
                final View child = parent.getChildAt(i);
                android.support.v7.widget.RecyclerView v = new android.support.v7.widget.RecyclerView(parent.getContext());
                final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child
                        .getLayoutParams();
                final int top = child.getBottom() + params.bottomMargin;
                final int bottom = top + mDivider.getIntrinsicHeight();
                mDivider.setBounds(left, top, right, bottom);
                mDivider.draw(c);
            }
        }

        public void drawHorizontal(Canvas c, RecyclerView parent, RecyclerView.State state) {
            final int top = parent.getPaddingTop();
            final int bottom = parent.getHeight() - parent.getPaddingBottom();

            final int childCount = parent.getChildCount();
            for (int i = 0; i < childCount; i++) {
                final View child = parent.getChildAt(i);
                final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child
                        .getLayoutParams();
                final int left = child.getRight() + params.rightMargin;
                final int right = left + mDivider.getIntrinsicHeight();
                mDivider.setBounds(left, top, right, bottom);
                mDivider.draw(c);
            }
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            if (mOrientation == VERTICAL_LIST) {
                outRect.set(0, 0, 0, mDivider.getIntrinsicHeight());
            } else {
                outRect.set(0, 0, mDivider.getIntrinsicWidth(), 0);
            }
        }
    }

    private class ServerThreadMessageHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            NotificationCompat.Builder nBuilder = new NotificationCompat.Builder(MainActivity.this).setSmallIcon(R.mipmap.ic_launcher).setContentTitle(getString(R.string.app_name));
            AlertDialog.Builder aBuilder = new AlertDialog.Builder(MainActivity.this);
            Bundle bundle = msg.getData();
            switch (bundle.getInt(Constants.SERVER_THREAD_STATUS)) {
                case Constants.SERVER_THREAD_STATUS_LISTENING:
                    nBuilder.setContentText(getString(R.string.listening) + " " + bundle.getString(Constants.IP_ADDRESS));
                    nBuilder.setOngoing(true);
                    break;
                case Constants.SERVER_THREAD_STATUS_WIFI_NOT_CONNECTED:
                    aBuilder.setIcon(R.mipmap.ic_launcher);
                    aBuilder.setTitle(getString(R.string.app_name));
                    aBuilder.setMessage(getString(R.string.wifi_not_connected));
                    aBuilder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                            notificationManager.cancelAll();
                            System.exit(0);
                        }
                    });
                    aBuilder.setCancelable(false);
                    aBuilder.show();
                    return;
                case Constants.SERVER_THREAD_STATUS_INIT_FAILED:
                    nBuilder.setContentText(getString(R.string.init_failed));
                    break;
                default:
                    nBuilder.setContentText(getString(R.string.unknown_error));
                    break;
            }
            NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(Constants.SERVER_NOTIFICATION_ID, nBuilder.build());
        }
    }

    private class ServerMessageHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Bundle bundle = msg.getData();
            switch (bundle.getInt(Constants.SERVERS_STATUS)) {
                case Constants.SERVERS_STATUS_FILE_WAIT_FOR_ACCEPT:
                    AlertDialog.Builder aBuilder = new AlertDialog.Builder(MainActivity.this);
                    aBuilder.setIcon(R.mipmap.ic_launcher);
                    aBuilder.setTitle(getString(R.string.app_name));
                    aBuilder.setMessage(getString(R.string.do_you_want_to_receive) + " " + bundle.getString(Constants.SERVER_FILENAME) + " " + getString(R.string.from) + " " + bundle.getString(Constants.SERVER_HOST));
                    aBuilder.setPositiveButton(getString(R.string.ok), new PositiveOnClickListener(bundle.getString(Constants.SERVER_HOST)));
                    aBuilder.setNegativeButton(getString(R.string.cancel), new NegativeOnClickListener(bundle.getString(Constants.SERVER_HOST)));
                    aBuilder.setCancelable(false);
                    aBuilder.show();
                    break;
                default:
                    break;
            }
        }

        private class PositiveOnClickListener implements DialogInterface.OnClickListener {
            public PositiveOnClickListener(String host) {
                this.host = host;
            }

            @Override
            public void onClick(DialogInterface dialog, int which) {
                for (Server server:servers)
                    if (server.host.equals(host)) {
                        server.status = Constants.SERVER_STATUS_FILE_ACCEPTED;
                        break;
                    }
            }

            private String host;
        }

        private class NegativeOnClickListener implements DialogInterface.OnClickListener {
            public NegativeOnClickListener(String host) {
                this.host = host;
            }

            @Override
            public void onClick(DialogInterface dialog, int which) {
                for (Server server:servers)
                    if (server.host.equals(host)) {
                        server.status = Constants.SERVER_STATUS_FILE_REJECTED;
                        break;
                    }
            }

            private String host;
        }
    }

    private class ClientMessageHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Bundle bundle = msg.getData();
            switch (bundle.getInt(Constants.CLIENTS_STATUS)) {
                case Constants.CLIENTS_STATUS_UPDATE_STATUS:
                    clientsAdapter.notifyDataSetChanged();
                    break;
                case Constants.CLIENTS_STATUS_RECEIVE_FILE_SUCCESS:
                    clientsAdapter.notifyDataSetChanged();
                    Toast.makeText(MainActivity.this.getApplicationContext(), getString(R.string.file) + " " + bundle.getString(Constants.RECEIVED_FILE_NAME) +
                            " " + getString(R.string.has_been_saved_at) + " " + bundle.getString(Constants.RECEIVED_FILE_PATH), Toast.LENGTH_LONG).show();
                    break;
                case Constants.CLIENTS_STATUS_RECEIVE_FILE_FAILED:
                    clientsAdapter.notifyDataSetChanged();
                    Toast.makeText(MainActivity.this.getApplicationContext(), getString(R.string.file) + " " + bundle.getString(Constants.RECEIVED_FILE_NAME) +
                            " " + getString(R.string.failed_to_receive), Toast.LENGTH_LONG).show();
                    break;
                default:
                    break;
            }
        }
    }

    private class ServerThread extends Thread {
        public ServerThread() {
            bundle = new Bundle();
            msg = new Message();
        }
        @Override
        public void run() {
            ConnectivityManager connMgr = (ConnectivityManager)
                    getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
            if ((networkInfo == null) || (!networkInfo.isConnected()) || (networkInfo.getType() != ConnectivityManager.TYPE_WIFI)) {
                bundle.putInt(Constants.SERVER_THREAD_STATUS, Constants.SERVER_THREAD_STATUS_WIFI_NOT_CONNECTED);
                msg.setData(bundle);
                MainActivity.this.serverThreadMessageHandler.sendMessage(msg);
                return;
            }
            WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            int ipAddress = wifiInfo.getIpAddress();
            String ip = (ipAddress & 0xff) + "." + (ipAddress >> 8 & 0xff) + "." + (ipAddress >> 16 & 0xff) + "." + (ipAddress >> 24 & 0xff);
            try {
                serverSocket = new ServerSocket(Constants.SERVER_PORT);
                bundle.putInt(Constants.SERVER_THREAD_STATUS, Constants.SERVER_THREAD_STATUS_LISTENING);
                bundle.putString(Constants.IP_ADDRESS, ip);
                msg.setData(bundle);
                MainActivity.this.serverThreadMessageHandler.sendMessage(msg);
            }
            catch (Exception e) {
                bundle.putInt(Constants.SERVER_THREAD_STATUS, Constants.SERVER_THREAD_STATUS_INIT_FAILED);
                msg.setData(bundle);
                MainActivity.this.serverThreadMessageHandler.sendMessage(msg);
                return;
            }
            Socket clientSocket;
            while (true) {
                try {
                    clientSocket = serverSocket.accept();
                    String hostAddress = clientSocket.getInetAddress().getHostAddress();
                    boolean inClients = false;
                    for (Client client:clients)
                        if (client.host.equals(hostAddress)) {
                            inClients = true;
                            break;
                        }
                    if (!inClients) {
                        Client newClient = new Client(hostAddress);
                        clients.add(newClient);
                        newClient.connect();
                    }
                    Server server = new Server(clientSocket, hostAddress);
                    servers.add(server);
                    server.start();
                }
                catch (Exception e) {
                }
            }
        }

        private ServerSocket serverSocket;
        private Bundle bundle;
        private Message msg;
    }

    private class Server extends Thread {
        public Server(Socket client, String host) {
            try {
                socket = client;
                socket_in = socket.getInputStream();
                socket_out = socket.getOutputStream();
                this.host = host;
                status = Constants.SERVER_STATUS_CONNECTED;
                buffer = new byte[Constants.BUFFER_SIZE];
            }
            catch (Exception e) {

            }
        }

        public void deleteThis() {
            try {
                socket.close();
            }
            catch (Exception e) {

            }
            status = Constants.SERVER_STATUS_CONNECT_FAILED;
            servers.remove(this);
        }

        private String getSocketIn() {
            try {
                String s = "";
                while (true) {
                    int count = socket_in.read(buffer);
                    if (count < 0) {
                        for (Iterator<Client> it = clients.iterator(); it.hasNext();) {
                            Client client = it.next();
                            if (client.host.equals(host)) {
                                client.deleteThis();
                                break;
                            }
                        }
                        deleteThis();
                        Bundle bundle = new Bundle();
                        bundle.putInt(Constants.CLIENTS_STATUS, Constants.CLIENTS_STATUS_UPDATE_STATUS);
                        Message msg = new Message();
                        msg.setData(bundle);
                        MainActivity.this.clientMessageHandler.sendMessage(msg);
                        return null;
                    }
                    s += new String(buffer, 0, count);
                    if (s.endsWith("\n")) {
                        s = s.substring(0, s.length() - 1);
                        break;
                    }
                }
                return s;
            }
            catch (Exception e) {
                return null;
            }
        }

        private void putSocketOut(String s) {
            try {
                socket_out.write(s.getBytes());
            }
            catch (Exception e) {

            }
        }

        private boolean portAvailable(int port) {
            try {
                ServerSocket server = new ServerSocket(port);
                server.close();
                return true;
            }
            catch (Exception e) {
                return false;
            }
        }

        private void receiveFile() {
            Client myClient = null;
            Bundle bundle = new Bundle();
            Message msg;
            String filename = "";
            try {
                String cmd = getSocketIn();
                if (cmd != null) {
                    String[] s = cmd.split(" ");
                    if (s[0].equals(Constants.CLIENT_COMMAND_SEND_FILE + "")) {
                        filename = s[1];
                        for (int i = 2; i < s.length; i++)
                            filename = filename + " " + s[i];
                        status = Constants.SERVER_STATUS_FILE_WAIT_FOR_ACCEPT;
                        bundle.putInt(Constants.SERVERS_STATUS, Constants.SERVERS_STATUS_FILE_WAIT_FOR_ACCEPT);
                        bundle.putString(Constants.SERVER_HOST, host);
                        bundle.putString(Constants.SERVER_FILENAME, filename);
                        msg = new Message();
                        msg.setData(bundle);
                        MainActivity.this.serverMessageHandler.sendMessage(msg);
                        while (status == Constants.SERVER_STATUS_FILE_WAIT_FOR_ACCEPT)
                            ;
                        if (status != Constants.SERVER_STATUS_FILE_ACCEPTED) {
                            putSocketOut(Constants.SERVER_COMMAND_REJECT_FILE + "\n");
                            return;
                        }
                        for (Client client:clients)
                            if (client.host.equals(host)) {
                                myClient = client;
                                break;
                            }
                        if (myClient == null) {
                            putSocketOut(Constants.SERVER_COMMAND_REJECT_FILE + "\n");
                            return;
                        }
                        myClient.status = Constants.CLIENT_STATUS_RECEIVING_FILE;
                        bundle.clear();
                        bundle.putInt(Constants.CLIENTS_STATUS, Constants.CLIENTS_STATUS_UPDATE_STATUS);
                        msg = new Message();
                        msg.setData(bundle);
                        MainActivity.this.clientMessageHandler.sendMessage(msg);
                        Random random = new Random();
                        int file_port;
                        while (true) {
                            file_port = random.nextInt(40000) + 20000;
                            if (portAvailable(file_port))
                                break;
                        }
                        ServerSocket file_server_socket = new ServerSocket(file_port);
                        putSocketOut(Constants.SERVER_COMMAND_ACCEPT_FILE + " " + file_port + "\n");
                        Socket file_socket = file_server_socket.accept();
                        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                        path.mkdirs();
                        File file = new File(path, filename);
                        FileOutputStream out = new FileOutputStream(file);
                        InputStream in = file_socket.getInputStream();
                        byte[] file_buffer = new byte[Constants.BUFFER_SIZE];
                        while (true) {
                            int count = in.read(file_buffer);
                            if (count < 0)
                                break;
                            out.write(file_buffer, 0, count);
                        }
                        out.close();
                        file_socket.close();
                        file_server_socket.close();
                        putSocketOut(Constants.SERVER_COMMAND_RECEIVE_FILE_SUCCESS + "\n");
                        myClient.status = Constants.CLIENT_STATUS_RECEIVE_FILE_DONE;
                        bundle.putInt(Constants.CLIENTS_STATUS, Constants.CLIENTS_STATUS_RECEIVE_FILE_SUCCESS);
                        bundle.putString(Constants.RECEIVED_FILE_PATH, path.getAbsolutePath());
                        bundle.putString(Constants.RECEIVED_FILE_NAME, filename);
                        msg = new Message();
                        msg.setData(bundle);
                        MainActivity.this.clientMessageHandler.sendMessage(msg);
                    }
                }
            }
            catch (Exception e) {
                putSocketOut(Constants.SERVER_COMMAND_RECEIVE_FILE_FAILED + "\n");
                if (myClient != null)
                    myClient.status = Constants.CLIENT_STATUS_RECEIVE_FILE_FAILED;
                bundle.putInt(Constants.CLIENTS_STATUS, Constants.CLIENTS_STATUS_RECEIVE_FILE_FAILED);
                bundle.putString(Constants.RECEIVED_FILE_NAME, filename);
                msg = new Message();
                msg.setData(bundle);
                MainActivity.this.clientMessageHandler.sendMessage(msg);
            }
        }

        public void run() {
            while (status != Constants.SERVER_STATUS_CONNECT_FAILED) {
                receiveFile();
            }
        }

        public String host;
        private int status;
        private Socket socket;
        private InputStream socket_in;
        private OutputStream socket_out;
        private byte[] buffer;
    }

    private class Client {
        public Client(String host) {
            this.host = host;
            status = Constants.CLIENT_STATUS_CONNECTING;
            buffer = new byte[Constants.BUFFER_SIZE];
        }

        public void deleteThis() {
            try {
                socket.close();
            }
            catch (Exception e) {

            }
            status = Constants.CLIENT_STATUS_CONNECT_FAILED;
            clients.remove(this);
        }

        private String getSocketIn() {
            try {
                String s = "";
                while (true) {
                    int count = socket_in.read(buffer);
                    if (count < 0) {
                        for (Iterator<Server> it = servers.iterator(); it.hasNext();) {
                            Server server = it.next();
                            if (server.host.equals(host)) {
                                server.deleteThis();
                                break;
                            }
                        }
                        deleteThis();
                        Bundle bundle = new Bundle();
                        bundle.putInt(Constants.CLIENTS_STATUS, Constants.CLIENTS_STATUS_UPDATE_STATUS);
                        Message msg = new Message();
                        msg.setData(bundle);
                        MainActivity.this.clientMessageHandler.sendMessage(msg);
                        return null;
                    }
                    s += new String(buffer, 0, count);
                    if (s.endsWith("\n")) {
                        s = s.substring(0, s.length() - 1);
                        break;
                    }
                }
                return s;
            }
            catch (Exception e) {
                return null;
            }
        }

        private void putSocketOut(String s) {
            try {
                socket_out.write(s.getBytes());
            }
            catch (Exception e) {

            }
        }

        public void connect() {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        socket = new Socket(host, Constants.SERVER_PORT);
                        socket_in = socket.getInputStream();
                        socket_out = socket.getOutputStream();
                        status = Constants.CLIENT_STATUS_CONNECTED;
                    } catch (Exception e) {
                        status = Constants.CLIENT_STATUS_CONNECT_FAILED;
                    }
                    Bundle bundle = new Bundle();
                    bundle.putInt(Constants.CLIENTS_STATUS, Constants.CLIENTS_STATUS_UPDATE_STATUS);
                    Message msg = new Message();
                    msg.setData(bundle);
                    MainActivity.this.clientMessageHandler.sendMessage(msg);
                }
            }).start();
        }

        public void sendFile(String path) {
            filepath = path;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        status = Constants.CLIENT_STATUS_SENDING_FILE;
                        Bundle bundle = new Bundle();
                        bundle.putInt(Constants.CLIENTS_STATUS, Constants.CLIENTS_STATUS_UPDATE_STATUS);
                        Message msg = new Message();
                        msg.setData(bundle);
                        MainActivity.this.clientMessageHandler.sendMessage(msg);
                        String[] s = filepath.split("/");
                        String filename = s[s.length - 1];
                        putSocketOut(Constants.CLIENT_COMMAND_SEND_FILE + " " + filename + "\n");
                        String t = getSocketIn();
                        if (t != null && t.startsWith(Constants.SERVER_COMMAND_ACCEPT_FILE + "")) {
                            int port = Integer.parseInt((t.split(" "))[1]);
                            Socket file_socket = new Socket(host, port);
                            File file = new File(filepath);
                            FileInputStream in = new FileInputStream(file);
                            OutputStream out = file_socket.getOutputStream();
                            byte[] file_buffer = new byte[Constants.BUFFER_SIZE];
                            int count;
                            while (true) {
                                count = in.read(file_buffer);
                                if (count < 0)
                                    break;
                                out.write(file_buffer, 0, count);
                            }
                            in.close();
                            file_socket.close();
                            if (getSocketIn().equals(Constants.SERVER_COMMAND_RECEIVE_FILE_SUCCESS + ""))
                                status = Constants.CLIENT_STATUS_SEND_FILE_DONE;
                            else
                                status = Constants.CLIENT_STATUS_SEND_FILE_FAILED;
                        }
                        else
                            status = Constants.CLIENT_STATUS_SEND_FILE_FAILED;
                        msg = new Message();
                        msg.setData(bundle);
                        MainActivity.this.clientMessageHandler.sendMessage(msg);
                    }
                    catch (Exception e) {

                    }
                }
            }).start();
        }

        public String host;
        public int status;
        private Socket socket;
        private InputStream socket_in;
        private OutputStream socket_out;
        private byte[] buffer;
        private String filepath;
    }

    private ServerThreadMessageHandler serverThreadMessageHandler;
    private ServerMessageHandler serverMessageHandler;
    private ClientMessageHandler clientMessageHandler;
    private ServerThread serverThread;
    private ArrayList<Server> servers;
    private ArrayList<Client> clients;
    private ClientsAdapter clientsAdapter;
    private int currentClient;
}
