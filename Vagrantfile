# -*- mode: ruby -*-
# vi: set ft=ruby :
Vagrant.configure("2") do |config|

  config.vm.define "windows" do |windows|
    windows.vm.box = "opentable/win-2012r2-standard-amd64-nocm"
  end

  config.vm.define "linux" do |linux|
    linux.vm.box = "centos/7"

    linux.vm.provision "shell", inline: <<-SHELL
      yum install -y java-11-openjdk-headless
    SHELL
  end
end

