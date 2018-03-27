package se.esss.ics.masar.services.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import se.esss.ics.masar.epics.IEpicsService;
import se.esss.ics.masar.epics.exception.PVReadException;
import se.esss.ics.masar.model.config.Config;
import se.esss.ics.masar.model.config.ConfigPv;
import se.esss.ics.masar.model.exception.ConfigNotFoundException;
import se.esss.ics.masar.model.exception.SnapshotNotFoundException;
import se.esss.ics.masar.model.node.Node;
import se.esss.ics.masar.model.snapshot.Snapshot;
import se.esss.ics.masar.model.snapshot.SnapshotPv;
import se.esss.ics.masar.persistence.dao.ConfigDAO;
import se.esss.ics.masar.persistence.dao.SnapshotDAO;
import se.esss.ics.masar.services.IServices;


public class Services implements IServices{

	@Autowired
	private ConfigDAO configDAO;
	
	@Autowired
	private SnapshotDAO snapshotDAO;
	
	@Autowired
	private IEpicsService epicsService;
	
	@Override
	@Transactional
	public Node createNewConfiguration(Config config) {
		return configDAO.createNewConfiguration(config);
	}

	@Override
	public List<Config> getConfigs(){
		return configDAO.getConfigs();
	}
	
	@Override
	public Config getConfig(int configId){
		Config config = configDAO.getConfig(configId);
		if(config == null) {
			throw new ConfigNotFoundException("Config with id=" + configId  + " not found.");
		}
		
		return config;
	}
	
	@Override
	@Transactional
	@SuppressWarnings("rawtypes")
	public Snapshot takeSnapshot(int configId) {
		
		Config config = configDAO.getConfig(configId);
		
		if(config == null) {
			throw new ConfigNotFoundException("Cannot take snapshot for config with id=" + configId  + " as it does not exist.");
		}
		
		Snapshot snapshot = Snapshot.builder()
				.configId(configId)
				.build();
		
		List<SnapshotPv<?>> snapshotPvs = new ArrayList<>();
		
		for(ConfigPv configPv : config.getConfigPvList()) {
			try {
				snapshotPvs.add(epicsService.getPv(configPv.getPvName()));
			} catch (PVReadException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		snapshot.setSnapshotPvList(snapshotPvs);
		
		return configDAO.savePreliminarySnapshot(snapshot);
	
	}
	
	@Override
	public List<SnapshotPv<?>> getSnapshotPvValues(int snapshotId){
		return snapshotDAO.getSnapshotPvValues(snapshotId);
	}
	
	@Override
	public Snapshot commitSnapshot(int snapshotId, String userName, String comment) {
		snapshotDAO.commitSnapshot(snapshotId, userName, comment);
		
		return snapshotDAO.getSnapshot(snapshotId);
	}
	
	@Override
	public void deleteSnapshot(int snapshotId) {
		snapshotDAO.deleteSnapshot(snapshotId);
	}
	
	@Override
	public List<Snapshot> getSnapshots(int configId){
		return snapshotDAO.getSnapshots(configId);
	}
	
	@Override
	public Snapshot getSnapshot(int snapshotId){
		Snapshot snapshot = snapshotDAO.getSnapshot(snapshotId);
		if(snapshot == null) {
			throw new SnapshotNotFoundException("Snapshot with id=" + snapshotId  + " not found.");
		}
		return snapshot;
	}
	
	@Override
	public Node createNewFolder(Node node) {
		
		if(node.getParent() == null) {
			throw new IllegalArgumentException("Cannot create new folder as parent folder is not specified.");
		}
	
		return configDAO.createNewFolder(node);
	}
	
	@Override
	public Node getNode(int nodeId) {
		
		Node node = configDAO.getNode(nodeId);
		if(node == null) {
			throw new IllegalArgumentException("No node found with id=" + nodeId);
		}
		
		return node;
	}
}
