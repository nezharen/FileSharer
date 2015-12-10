#include <QtGui>
#include "ConnectDialog.h"

ConnectDialog::ConnectDialog()
{
	ipLabel = new QLabel(tr("&IP:"));
	ipEdit = new QLineEdit;
	ipEdit->setAlignment(Qt::AlignHCenter);
	ipEdit->setInputMask("000.000.000.000");
	ipLabel->setBuddy(ipEdit);
	topLayout = new QHBoxLayout;
	topLayout->addWidget(ipLabel);
	topLayout->addWidget(ipEdit);

	okButton = new QPushButton(tr("&OK"));
	okButton->setDefault(true);
	exitButton = new QPushButton(tr("&Exit"));
	connect(exitButton, SIGNAL(clicked()), this, SLOT(close()));
	buttonLayout = new QHBoxLayout;
	buttonLayout->addWidget(okButton);
	buttonLayout->addStretch();
	buttonLayout->addWidget(exitButton);

	mainLayout = new QVBoxLayout;
	mainLayout->addLayout(topLayout);
	mainLayout->addLayout(buttonLayout);

	setLayout(mainLayout);
	setWindowTitle(tr("FileSharer"));
	setFixedSize(sizeHint());
}
