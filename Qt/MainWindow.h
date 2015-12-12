#ifndef MAIN_WINDOW_H
#define MAIN_WINDOW_H

#include <QMainWindow>

class QTableWidget;
class QTableWidgetItem;
class ConnectDialog;
class QTcpServer;
class Server;
class Client;

class MainWindow : public QMainWindow
{
	Q_OBJECT
public:
	MainWindow();
protected slots:
	void showAbout();
	void connectHost();
	void acceptNewConnection();
	void newClient(const QString &hostAddress);
	void updateClientsTable();
	void selectFileToSend(QTableWidgetItem *item);
	void closeServerConnection(Server *server);
	void closeClientConnection(Client *client);
	void askForReceive(Server *server);
	void fileReceiveSuccess(Server *server);
	void fileReceiveFailed(Server *server);
private:
	QMenu *startMenu, *aboutMenu;
	QAction *connectAction, *exitAction, *aboutAction, *aboutQtAction;
	QTableWidget *clientsTable;
	ConnectDialog *connectDialog;
	QTcpServer *mainServerSocket;
	QList<Server*> *servers;
	QList<Client*> *clients;
};

#endif
