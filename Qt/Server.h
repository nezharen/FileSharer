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
signals:
	void serverConnectionClosed(Server *server);
protected slots:
	void closeConnection();
private:
	QTcpSocket *socket;
};

#endif
