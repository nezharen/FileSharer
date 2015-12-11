#include <QtNetwork>
#include "Server.h"

Server::Server(QTcpSocket *socket)
{
	this->socket = socket;
	host = socket->peerAddress().toString();
	connect(this->socket, SIGNAL(disconnected()), this, SLOT(closeConnection()));
}

void Server::closeConnection()
{
	emit serverConnectionClosed(this);
}
