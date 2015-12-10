#include <iostream>
#include <QtNetwork>
#include "defs.h"
#include "MainServer.h"

MainServer::MainServer()
{
	mainServerSocket = new QTcpServer(this);
	connect(mainServerSocket, SIGNAL(newConnection()), this, SLOT(acceptNewConnection()));
	if (!(mainServerSocket->listen(QHostAddress::Any, SERVER_PORT)))
	{
		std::cerr << "Cannot listen port: " << SERVER_PORT << std::endl;
		qApp->quit();
	}
}

void MainServer::acceptNewConnection()
{
	mainServerSocket->nextPendingConnection();
}
