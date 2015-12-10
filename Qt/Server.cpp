#include <QtNetwork>
#include "Server.h"

Server::Server(QTcpSocket *socket)
{
	this->socket = socket;
	host = socket->peerAddress().toString();
}
