#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <ctime>
#include <QtNetwork>
#include "defs.h"
#include "Server.h"

Server::Server(QTcpSocket *socket)
{
	this->socket = socket;
	host = socket->peerAddress().toString();
	connect(this->socket, SIGNAL(disconnected()), this, SLOT(closeConnection()));
	connect(this->socket, SIGNAL(readyRead()), this, SLOT(readCommand()));
	file_socket_server = new QTcpServer(this);
	connect(file_socket_server, SIGNAL(newConnection()), this, SLOT(readyToReceiveFile()));
	commandLength = 0;
}

void Server::closeConnection()
{
	emit serverConnectionClosed(this);
}

void Server::readCommand()
{
	QDataStream in(socket);
	in.setVersion(QDataStream::Qt_4_3);
	forever
	{
		if (socket->bytesAvailable() <= 0)
			break;
		int count = in.readRawData(buffer, BUFFER_SIZE);
		for (int i = 0; i < count; i++)
			command[commandLength++] = buffer[i];
		if (command[commandLength - 1] == '\n')
		{
			commandLength--;
			int commandCode = getInt(command, 0, 2);
			switch (commandCode)
			{
				case CLIENT_COMMAND_SEND_FILE:
					filename = tr("");
					for (int i = 4; i < commandLength; i++)
						filename.append(QChar(command[i]));
					emit receiveFileRequest(this);
					break;
				default:
					break;
			}
			commandLength = 0;
		}
	}
}

void Server::acceptFile()
{
	srand((int)time(0));
	int port = rand() % 40000 + 20000;
	while (true)
	{
		if (file_socket_server->listen(QHostAddress::Any, port))
			break;
		port = rand() % 40000 + 20000;
	}
	QByteArray block;
	QDataStream out(&block, QIODevice::WriteOnly);
	out.setVersion(QDataStream::Qt_4_3);
	char s[BUFFER_SIZE];
	sprintf(s, "%d %d\n", SERVER_COMMAND_ACCEPT_FILE, port);
	out.writeRawData(s, strlen(s));
	socket->write(block);
}

void Server::rejectFile()
{
	QByteArray block;
	QDataStream out(&block, QIODevice::WriteOnly);
	out.setVersion(QDataStream::Qt_4_3);
	char s[BUFFER_SIZE];
	sprintf(s, "%d\n", SERVER_COMMAND_REJECT_FILE);
	out.writeRawData(s, strlen(s));
	socket->write(block);
}

void Server::readyToReceiveFile()
{
	QFile file(filename);
	file.remove();
	file_socket = file_socket_server->nextPendingConnection();
	connect(file_socket, SIGNAL(disconnected()), this, SLOT(receiveFileSuccess()));
	connect(file_socket, SIGNAL(readyRead()), this, SLOT(receiveFile()));
}

void Server::receiveFile()
{
	QDataStream in(file_socket);
	in.setVersion(QDataStream::Qt_4_3);
	QFile file(filename);
	if (!file.open(QIODevice::Append))
	{
		receiveFileFailed();
		return;
	}
	QDataStream out(&file);
	out.setVersion(QDataStream::Qt_4_3);
	forever
	{
		if (file_socket->bytesAvailable() <= 0)
			break;
		int count = in.readRawData(file_buffer, BUFFER_SIZE);
		out.writeRawData(file_buffer, count);
	}
}

void Server::receiveFileSuccess()
{
	file_socket->close();
	file_socket_server->close();
	QByteArray block;
	QDataStream out(&block, QIODevice::WriteOnly);
	out.setVersion(QDataStream::Qt_4_3);
	char s[BUFFER_SIZE];
	sprintf(s, "%d\n", SERVER_COMMAND_RECEIVE_FILE_SUCCESS);
	out.writeRawData(s, strlen(s));
	socket->write(block);
	emit fileReceiveSuccess(this);
}

void Server::receiveFileFailed()
{
	file_socket->close();
	file_socket_server->close();
	QByteArray block;
	QDataStream out(&block, QIODevice::WriteOnly);
	out.setVersion(QDataStream::Qt_4_3);
	char s[BUFFER_SIZE];
	sprintf(s, "%d\n", SERVER_COMMAND_RECEIVE_FILE_FAILED);
	out.writeRawData(s, strlen(s));
	socket->write(block);
	emit fileReceiveFailed(this);
}
