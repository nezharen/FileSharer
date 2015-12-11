#include <QtNetwork>
#include "defs.h"
#include "Client.h"

Client::Client(QString *host)
{
	this->host = host;
	status = CLIENT_STATUS_CONNECTING;
	socket = new QTcpSocket(this);
	connect(socket, SIGNAL(connected()), this, SLOT(connected()));
}

void Client::connectHost()
{
	socket->connectToHost(*host, SERVER_PORT);
}

void Client::connected()
{
	status = CLIENT_STATUS_CONNECTED;
}
