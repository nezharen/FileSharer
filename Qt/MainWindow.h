#ifndef MAIN_WINDOW_H
#define MAIN_WINDOW_H

#include <QMainWindow>

class ConnectDialog;
class QTcpServer;
class Server;

class MainWindow : public QMainWindow
{
	Q_OBJECT
public:
	MainWindow();
protected slots:
	void connectHost();
	void acceptNewConnection();
private:
	QMenu *startMenu;
	QAction *connectAction;
	ConnectDialog *connectDialog;
	QTcpServer *mainServerSocket;
	QList<Server*> *serverList;
};

#endif
