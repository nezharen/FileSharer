#ifndef MAIN_SERVER_H
#define MAIN_SERVER_H

#include <QObject>

class QTcpServer;

class MainServer : QObject
{
	Q_OBJECT
public:
	MainServer();
protected slots:
	void acceptNewConnection();
private:
	QTcpServer *mainServerSocket;
};

#endif
