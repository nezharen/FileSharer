#ifndef CLIENT_H
#define CLIENT_H

#include <QObject>
#include "defs.h"

class QTcpSocket;

class Client : public QObject
{
	Q_OBJECT
public:
	Client(QString host);
	void connectHost();
	void sendFile(QString path);
	QString host;
	int status;
signals:
	void statusChanged();
	void clientConnectionClosed(Client *client);
protected slots:
	void connected();
	void closeConnection();
	void readCommand();
	void sendFileContent();
private:
	QTcpSocket *socket, *file_socket;
	QString path;
	char command[BUFFER_SIZE];
	int commandLength;
	char buffer[BUFFER_SIZE];
};

#endif
