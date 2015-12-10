#ifndef SERVER_H
#define SERVER_H

#include <QObject>

class QTcpSocket;

class Server : public QObject
{
	Q_OBJECT
public:
	Server(QTcpSocket *socket);
	QString host;
	int status;
private:
	QTcpSocket *socket;
};

#endif
