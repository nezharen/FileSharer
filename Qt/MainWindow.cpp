#include <iostream>
#include <QtGui>
#include <QtNetwork>
#include "defs.h"
#include "Server.h"
#include "Client.h"
#include "ConnectDialog.h"
#include "MainWindow.h"

MainWindow::MainWindow()
{
	connectAction = new QAction(tr("&Connect"), this);
	connectAction->setStatusTip(tr("Connect a host to share file with it."));
	connect(connectAction, SIGNAL(triggered()), this, SLOT(connectHost()));
	exitAction = new QAction(tr("&Exit"), this);
	exitAction->setStatusTip(tr("Exit."));
	connect(exitAction, SIGNAL(triggered()), qApp, SLOT(quit()));
	aboutAction = new QAction(tr("&About FileSharer"), this);
	aboutAction->setStatusTip(tr("Show the program's about box."));
	connect(aboutAction, SIGNAL(triggered()), this, SLOT(showAbout()));
	aboutQtAction = new QAction(tr("About Qt"), this);
	aboutQtAction->setStatusTip(tr("Show the Qt library's about box."));
	connect(aboutQtAction, SIGNAL(triggered()), qApp, SLOT(aboutQt()));
	startMenu = menuBar()->addMenu(tr("&Start"));
	startMenu->addAction(connectAction);
	startMenu->addAction(exitAction);
	aboutMenu = menuBar()->addMenu(tr("&About"));
	aboutMenu->addAction(aboutAction);
	aboutMenu->addAction(aboutQtAction);
	statusBar();
	clientsTable = new QTableWidget(this);
	clientsTable->setColumnCount(2);
	QStringList header;
	header << tr("IP") << tr("Status");
	clientsTable->setHorizontalHeaderLabels(header);
	clientsTable->horizontalHeader()->setResizeMode(QHeaderView::Stretch);
	clientsTable->verticalHeader()->setVisible(false);
	clientsTable->setEditTriggers(QAbstractItemView::NoEditTriggers);
	clientsTable->setSelectionBehavior(QAbstractItemView::SelectRows);
	setCentralWidget(clientsTable);
	setWindowTitle(tr("FileSharer"));
	connectDialog = NULL;
	servers = new QList<Server*>;
	clients = new QList<Client*>;
	mainServerSocket = new QTcpServer(this);
	connect(mainServerSocket, SIGNAL(newConnection()), this, SLOT(acceptNewConnection()));
	if (!(mainServerSocket->listen(QHostAddress::Any, SERVER_PORT)))
	{
		std::cerr << "Cannot listen port: " << SERVER_PORT << std::endl;
		qApp->quit();
	}
}

void MainWindow::showAbout()
{
	QMessageBox::about(this, tr("About FileSharer"), tr("<h2>FileSharer</h2><p>Click \"Connect\" to connect hosts.</p><p>Double click connected hosts to send file to it.</p>"));
}

void MainWindow::connectHost()
{
	if (connectDialog == NULL)
	{
		connectDialog = new ConnectDialog;
		connect(connectDialog, SIGNAL(newClient(const QString &)), this, SLOT(newClient(const QString &)));
	}

	connectDialog->show();
	connectDialog->raise();
	connectDialog->activateWindow();
}

void MainWindow::acceptNewConnection()
{
	Server *server = new Server(mainServerSocket->nextPendingConnection());
	connect(server, SIGNAL(serverConnectionClosed(Server *)), this, SLOT(closeServerConnection(Server *)));
	servers->append(server);
	bool inClients = false;
	foreach (Client *client, *clients)
		if (client->host == server->host)
		{
			inClients = true;
			break;
		}
	if (!inClients)
	{
		Client *newClient = new Client(server->host);
		connect(newClient, SIGNAL(statusChanged()), this, SLOT(updateClientsTable()));
		connect(newClient, SIGNAL(clientConnectionClosed(Client *)), this, SLOT(closeClientConnection(Client *)));
		clients->append(newClient);
		updateClientsTable();
		newClient->connectHost();
	}
}

void MainWindow::newClient(const QString &hostAddress)
{
	bool inClients = false;
	foreach (Client *client, *clients)
		if (client->host == hostAddress)
		{
			inClients = true;
			break;
		}
	if (!inClients)
	{
		Client *newClient = new Client(hostAddress);
		connect(newClient, SIGNAL(statusChanged()), this, SLOT(updateClientsTable()));
		connect(newClient, SIGNAL(clientConnectionClosed(Client *)), this, SLOT(closeClientConnection(Client *)));
		clients->append(newClient);
		updateClientsTable();
		newClient->connectHost();
	}
	else
		QMessageBox::critical(this, tr("FileSharer"), tr("Client is already added"));
}

void MainWindow::updateClientsTable()
{
	clientsTable->clearContents();
	clientsTable->setRowCount(clients->size());
	for (int i = 0; i < clients->size(); i++)
	{
		clientsTable->setItem(i, 0, new QTableWidgetItem(clients->at(i)->host));
		switch (clients->at(i)->status)
		{
			case CLIENT_STATUS_CONNECTING:
				clientsTable->setItem(i, 1, new QTableWidgetItem(tr("Connecting")));
				break;
			case CLIENT_STATUS_CONNECTED:
				clientsTable->setItem(i, 1, new QTableWidgetItem(tr("Connected")));
				break;
			default:
				clientsTable->setItem(i, 1, new QTableWidgetItem(tr("Unknown error")));
				break;
		}
	}
}

void MainWindow::closeServerConnection(Server *server)
{
	QListIterator<Client *> it(*clients);
	while (it.hasNext())
	{
		Client *client = it.next();
		if (client->host == server->host)
		{
			disconnect(client, 0, this, 0);
			clients->removeOne(client);
			break;
		}
	}
	disconnect(server, 0, this, 0);
	servers->removeOne(server);
	updateClientsTable();
}

void MainWindow::closeClientConnection(Client *client)
{
	QListIterator<Server *> it(*servers);
	while (it.hasNext())
	{
		Server *server = it.next();
		if (server->host == client->host)
		{
			disconnect(server, 0, this, 0);
			servers->removeOne(server);
			break;
		}
	}
	disconnect(client, 0, this, 0);
	clients->removeOne(client);
	updateClientsTable();
}
