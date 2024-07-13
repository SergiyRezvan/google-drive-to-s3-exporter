### install tools and checkout the project
sudo yum install git
git clone https://github.com/SergiyRezvan/google-drive-to-s3-exporter.git
sudo yum install java-17-amazon-corretto
sudo yum install maven

cd google-drive-to-s3-exporter
mvn clean package

### After adding exporter.service to /etc/systemd/system/
sudo chmod 644 /etc/systemd/system/exporter.service
sudo systemctl enable exporter.service
