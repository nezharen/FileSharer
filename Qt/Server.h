#ifndef SERVER_H
#define SERVER_H

#include <QObject>
#include "defs.h"

class QTcpServer;
class QTcpSocket;

class Server : public QObject
{
	Q_OBJECT
public:
	Server(QTcpSocket *socket);
	void acceptFile();
	void rejectFile();
	QString host;
	QString filename;
signals:
	void serverConnectionClosed(Server *server);
	void receiveFileRequest(Server *server);
	void fileReceiveSuccess(Server *server);
	void fileReceiveFailed(Server *server);
protected slots:
	void closeConnection();
	void readCommand();
	void readyToReceiveFile();
	void receiveFile();
	void receiveFileSuccess();
	void receiveFileFailed();
private:
	QTcpServer *file_socket_server;
	QTcpSocket *socket, *file_socket;
	char command[BUFFER_SIZE];
	int commandLength;
	char buffer[BUFFER_SIZE];
	char file_buffer[BUFFER_SIZE];
};

#endif
