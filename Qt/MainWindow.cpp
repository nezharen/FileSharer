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
	startMenu = menuBar()->addMenu(tr("&Start"));
	startMenu->addAction(connectAction);
	startMenu->addAction(exitAction);
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
	servers->append(server);
}

void MainWindow::newClient(const QString &hostAddress)
{
	bool inClients = false;
	foreach (Client *client, *clients)
		if (*(client->host) == hostAddress)
		{
			inClients = true;
			break;
		}
	if (!inClients)
	{
		Client *newClient = new Client(new QString(hostAddress));
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
		clientsTable->setItem(i, 0, new QTableWidgetItem(*(clients->at(i)->host)));
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
