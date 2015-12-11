#ifndef MAIN_WINDOW_H
#define MAIN_WINDOW_H

#include <QMainWindow>

class QTableWidget;
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
	void connectHost();
	void acceptNewConnection();
	void newClient(const QString &hostAddress);
	void updateClientsTable();
private:
	QMenu *startMenu;
	QAction *connectAction, *exitAction;
	QTableWidget *clientsTable;
	ConnectDialog *connectDialog;
	QTcpServer *mainServerSocket;
	QList<Server*> *servers;
	QList<Client*> *clients;
};

#endif
