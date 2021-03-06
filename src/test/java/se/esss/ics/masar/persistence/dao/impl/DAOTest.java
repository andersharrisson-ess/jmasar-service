package se.esss.ics.masar.persistence.dao.impl;

import static org.assertj.core.api.Assertions.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.flywaydb.test.FlywayTestExecutionListener;
import org.flywaydb.test.annotation.FlywayTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import se.esss.ics.masar.model.Config;
import se.esss.ics.masar.model.ConfigPv;
import se.esss.ics.masar.model.Folder;
import se.esss.ics.masar.model.Node;
import se.esss.ics.masar.model.Snapshot;
import se.esss.ics.masar.model.SnapshotPv;
import se.esss.ics.masar.persistence.config.PersistenceConfiguration;
import se.esss.ics.masar.persistence.config.PersistenceTestConfig;
import se.esss.ics.masar.persistence.dao.ConfigDAO;
import se.esss.ics.masar.persistence.dao.SnapshotDAO;
import se.esss.ics.masar.services.exception.NodeNotFoundException;

@RunWith(SpringJUnit4ClassRunner.class)
@EnableConfigurationProperties
@ContextHierarchy({ @ContextConfiguration(classes = { PersistenceConfiguration.class, PersistenceTestConfig.class }) })
@TestPropertySource(properties = { "dbengine = h2" })
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class, FlywayTestExecutionListener.class })
public class DAOTest {

	@Autowired
	private ConfigDAO configDAO;

	@Autowired
	private SnapshotDAO snapshotDAO;

	@Test(expected = NodeNotFoundException.class)
	@FlywayTest(invokeCleanDB = true)
	public void testCreateConfigNoParentFound() {

		ConfigPv configPv = ConfigPv.builder().groupname("groupname").pvName("pvName").readonly(true).tags("tags")
				.build();

		Config config = Config.builder().active(true).configPvList(Arrays.asList(configPv)).description("description")
				.system("system").build();

		Node parentNode = new Node();
		parentNode.setId(2);

		config.setParent(parentNode);
		// The parent node does not exist in the database, so this throws an exception
		configDAO.createConfiguration(config);
	}

	@Test
	@FlywayTest(invokeCleanDB = true)
	public void testNewFolder(){
		
		Folder root = configDAO.getFolder(Node.ROOT_NODE_ID);
		
		Date lastModified = root.getLastModified();
	
		Folder folder = Folder.builder().name("SomeFolder").parent(root)
				.build();

		Node newNode = configDAO.createFolder(folder);
		
		root = configDAO.getFolder(Node.ROOT_NODE_ID);

		assertNotNull(newNode);
		
		// Check that the parent folder's last modified date is updated
		assertTrue(root.getLastModified().getTime() > lastModified.getTime());
	}

	@Test(expected = IllegalArgumentException.class)
	@FlywayTest(invokeCleanDB = true)
	public void testNewFolderNoParent() {

		Folder folderFromClient = Folder.builder().name("SomeFolder").build();

		configDAO.createFolder(folderFromClient);

	}

	@Test(expected = IllegalArgumentException.class)
	@FlywayTest(invokeCleanDB = true)
	public void testNewFolderWithDuplicateName() {

		Folder folderFromClient = Folder.builder().name("SomeFolder").parent(configDAO.getFolder(Node.ROOT_NODE_ID))
				.build();

		// Create a new folder
		configDAO.createFolder(folderFromClient);

		// Try to create a new folder with the same name in the same parent directory
		configDAO.createFolder(folderFromClient);

	}

	@Test
	@FlywayTest(invokeCleanDB = true)
	public void testNewFolderNoDuplicateName() {

		Node parentNode = configDAO.getFolder(Node.ROOT_NODE_ID);

		Folder folder1 = Folder.builder().name("Folder 1").parent(parentNode).build();

		Folder folder2 = Folder.builder().name("Folder 2").parent(parentNode).build();

		// Create a new folder
		assertNotNull(configDAO.createFolder(folder1));

		// Try to create a new folder with a different name in the same parent directory
		assertNotNull(configDAO.createFolder(folder2));

	}

	@Test
	@FlywayTest(invokeCleanDB = true)
	public void testNewConfig() {

		Node parentNode = configDAO.getFolder(Node.ROOT_NODE_ID);

		ConfigPv configPv = ConfigPv.builder().groupname("groupname").pvName("pvName").tags("tags").build();

		Config config = Config.builder().active(true).description("description").system("system").parent(parentNode)
				.name("My config").configPvList(Arrays.asList(configPv)).build();

		Config newConfig = configDAO.createConfiguration(config);

		assertFalse(newConfig.getConfigPvList().isEmpty());

		int configPvId = newConfig.getConfigPvList().get(0).getId();

		config = Config.builder().active(true).description("description").system("system").parent(parentNode)
				.name("My config 2").configPvList(Arrays.asList(configPv)).build();

		newConfig = configDAO.createConfiguration(config);

		// Verify that a new ConfigPv has NOT been created

		assertEquals(configPvId, newConfig.getConfigPvList().get(0).getId());
	}

	@Test
	@FlywayTest(invokeCleanDB = true)
	public void testNewConfigNoConfigPvs() {

		Node node = configDAO.getFolder(Node.ROOT_NODE_ID);

		Config config = Config.builder().active(true).description("description").system("system").build();

		config.setParent(node);
		config.setName("My config");

		Config newConfig = configDAO.createConfiguration(config);

		assertTrue(newConfig.getConfigPvList().isEmpty());
	}

	@Test(expected = NodeNotFoundException.class)
	@FlywayTest(invokeCleanDB = true)
	public void testDeleteConfiguration() {

		Node parentNode = configDAO.getFolder(Node.ROOT_NODE_ID);

		Config config = Config.builder().active(true).description("description").system("system").build();

		config.setParent(parentNode);
		config.setName("My config");

		config = configDAO.createConfiguration(config);

		configDAO.deleteNode(config.getId());

		// Config deleted, this throws an exception
		configDAO.getConfiguration(config.getId());

	}

	@Test
	@FlywayTest(invokeCleanDB = true)
	public void testDeleteConfigurationAndPvs() {

		Node parentNode = configDAO.getFolder(Node.ROOT_NODE_ID);

		ConfigPv configPv = ConfigPv.builder().groupname("group").readonly(true).tags("tags").pvName("pvName").build();

		Config config = Config.builder().active(true).description("description").system("system")
				.configPvList(Arrays.asList(configPv)).build();

		config.setParent(parentNode);
		config.setName("My config");

		config = configDAO.createConfiguration(config);

		configDAO.deleteNode(config.getId());

	}

	@Test(expected = NodeNotFoundException.class)
	@FlywayTest(invokeCleanDB = true)
	public void testDeleteNonExistingFolder() {

		configDAO.deleteNode(-1);
	}

	@Test
	@FlywayTest(invokeCleanDB = true)
	public void testDeleteFolder() {

		Folder root = configDAO.getFolder(Node.ROOT_NODE_ID);

		Date rootLastModified = root.getLastModified();

		Folder folder1 = Folder.builder().name("SomeFolder").parent(root).build();

		folder1 = configDAO.createFolder(folder1);

		Folder folder2 = Folder.builder().name("SomeFolder").parent(folder1).build();

		folder2 = configDAO.createFolder(folder2);

		Config config = configDAO
				.createConfiguration(Config.builder().name("Config").description("Desc").parent(folder2).build());

		configDAO.deleteNode(folder1.getId());

		root = configDAO.getFolder(Node.ROOT_NODE_ID);
	
		assertTrue(root.getLastModified().getTime() > rootLastModified.getTime());

		try {
			configDAO.getConfiguration(config.getId());
			fail("NodeNotFoundException expected here.");
		} catch (NodeNotFoundException e) {
			// Expected = OK
		}

		try {
			configDAO.getFolder(folder2.getId());
			fail("NodeNotFoundException expected here.");
		} catch (NodeNotFoundException e) {
			// Expected = OK
		}
	}

	@Test
	@FlywayTest(invokeCleanDB = true)
	public void testDeleteConfigurationLeaveReferencedPVs() {

		Node parentNode = configDAO.getFolder(Node.ROOT_NODE_ID);

		ConfigPv configPv1 = ConfigPv.builder().groupname("group").readonly(true).tags("tags").pvName("pvName").build();

		ConfigPv configPv2 = ConfigPv.builder().groupname("group").readonly(true).tags("tags").pvName("pvName2")
				.build();

		Config config1 = Config.builder().active(true).description("description").system("system")
				.configPvList(Arrays.asList(configPv1, configPv2)).build();

		config1.setParent(parentNode);
		config1.setName("My config");

		config1 = configDAO.createConfiguration(config1);

		Config config2 = Config.builder().active(true).description("description").system("system")
				.configPvList(Arrays.asList(configPv2)).build();

		config2.setParent(parentNode);
		config2.setName("My config 2");

		config2 = configDAO.createConfiguration(config2);

		configDAO.deleteNode(config1.getId());

		Config config = configDAO.getConfiguration(config2.getId());

		assertEquals(1, config.getConfigPvList().size());

	}

	@Test
	@FlywayTest(invokeCleanDB = false)
	public void testGetNodeAsConfig() {

		Node parentNode = configDAO.getFolder(Node.ROOT_NODE_ID);

		Config config = Config.builder().active(true).name("My config 3").parent(parentNode).description("description")
				.system("system").build();

		Config newConfig = configDAO.createConfiguration(config);

		Config configFromDB = configDAO.getConfiguration(newConfig.getId());

		assertEquals(newConfig, configFromDB);
	}

	@Test
	@FlywayTest(invokeCleanDB = true)
	public void testSaveSnapshot() {

		Config config = Config.builder().active(true).name("My config 3").parent(configDAO.getFolder(Node.ROOT_NODE_ID))
				.description("description").system("system")
				.configPvList(Arrays.asList(ConfigPv.builder().pvName("whatever").build())).build();

		config = configDAO.createConfiguration(config);

		SnapshotPv<Integer> snapshotPv = SnapshotPv.<Integer>builder().dtype(1).fetchStatus(true).severity(2).status(3)
				.time(1000L).timens(20000).value(10)
				.configPv(ConfigPv.builder().pvName("whatever").id(config.getConfigPvList().get(0).getId()).build())
				.build();

		Snapshot snapshot = Snapshot.builder().approve(true).configId(config.getId())
				.snapshotPvList(Arrays.asList(snapshotPv)).build();

		Snapshot newSnapshot = configDAO.savePreliminarySnapshot(snapshot);

		assertEquals(10, newSnapshot.getSnapshotPvList().get(0).getValue());

		Snapshot fullSnapshot = snapshotDAO.getSnapshot(newSnapshot.getId(), true);

		assertNull(fullSnapshot);

		List<Snapshot> snapshots = snapshotDAO.getSnapshots(config.getId());

		assertTrue(snapshots.isEmpty());

		snapshotDAO.commitSnapshot(newSnapshot.getId(), "user", "comment");

		fullSnapshot = snapshotDAO.getSnapshot(newSnapshot.getId(), true);

		assertEquals(1, fullSnapshot.getSnapshotPvList().size());

		snapshots = snapshotDAO.getSnapshots(config.getId());

		assertEquals(1, snapshots.size());

		snapshotDAO.deleteSnapshot(newSnapshot.getId());

		snapshots = snapshotDAO.getSnapshots(config.getId());

		assertTrue(snapshots.isEmpty());

		newSnapshot = configDAO.savePreliminarySnapshot(snapshot);

		snapshotDAO.commitSnapshot(newSnapshot.getId(), "user", "comment");
	}

	@Test
	@FlywayTest(invokeCleanDB = true)
	public void testGetSnapshotsNoSnapshots() {

		List<Snapshot> snapshots = snapshotDAO.getSnapshots(-1);

		assertTrue(snapshots.isEmpty());
	}

	@Test(expected = NodeNotFoundException.class)
	@FlywayTest(invokeCleanDB = true)
	public void testGetNonexistingFolder() {

		configDAO.getFolder(-1);
	}

	@Test(expected = NodeNotFoundException.class)
	@FlywayTest(invokeCleanDB = true)
	public void testMoveNodeIllegalSource() {

		Folder folderFromClient = Folder.builder().name("SomeFolder").parent(configDAO.getFolder(Node.ROOT_NODE_ID))
				.build();

		Folder folder1 = configDAO.createFolder(folderFromClient);

		configDAO.moveNode(-1, folder1.getId());
	}

	@Test(expected = NodeNotFoundException.class)
	@FlywayTest(invokeCleanDB = true)
	public void testMoveNodeIllegalTarget() {

		Folder folderFromClient = Folder.builder().name("SomeFolder").parent(configDAO.getFolder(Node.ROOT_NODE_ID))
				.build();

		Folder folder1 = configDAO.createFolder(folderFromClient);

		configDAO.moveNode(folder1.getId(), -1);
	}

	@Test(expected = IllegalArgumentException.class)
	@FlywayTest(invokeCleanDB = true)
	public void testMoveNodeNameClash1() {

		Folder root = configDAO.getFolder(Node.ROOT_NODE_ID);

		Folder folder1 = Folder.builder().name("SomeFolder").parent(root).build();

		folder1 = configDAO.createFolder(folder1);

		Folder folder2 = Folder.builder().name("SomeFolder").parent(folder1).build();

		folder2 = configDAO.createFolder(folder2);

		configDAO.createConfiguration(Config.builder().name("Config").description("Desc").parent(folder2).build());

		configDAO.moveNode(folder2.getId(), root.getId());
	}

	@Test(expected = IllegalArgumentException.class)
	@FlywayTest(invokeCleanDB = true)
	public void testMoveNodeNameClash2() {

		Folder root = configDAO.getFolder(Node.ROOT_NODE_ID);

		configDAO.createConfiguration(Config.builder().name("Config").description("Desc").parent(root).build());

		Folder folder1 = configDAO.createFolder(Folder.builder().name("SomeFolder").parent(root).build());

		Config config2 = configDAO
				.createConfiguration(Config.builder().name("Config").description("Desc").parent(folder1).build());

		configDAO.moveNode(config2.getId(), root.getId());

	}

	@Test
	@FlywayTest(invokeCleanDB = true)
	public void testMoveNode()  {

		Folder root = configDAO.getFolder(Node.ROOT_NODE_ID);

		Folder folder1 = Folder.builder().name("SomeFolder").parent(root).build();

		folder1 = configDAO.createFolder(folder1);

		root = configDAO.getFolder(Node.ROOT_NODE_ID);

		// Root node has one child node
		assertEquals(1, root.getChildNodes().size());

		Folder folder2 = Folder.builder().name("SomeFolder2").parent(folder1).build();

		folder2 = configDAO.createFolder(folder2);

		folder1 = configDAO.getFolder(folder1.getId());

		// SomeFolder has one child node
		assertEquals(1, folder1.getChildNodes().size());

		configDAO.createConfiguration(Config.builder().name("Config").description("Desc").parent(folder2).build());

		Date lastModifiedOfSource = folder1.getLastModified();
		Date lastModifiedOfTarget = root.getLastModified();

		folder2 = configDAO.moveNode(folder2.getId(), Node.ROOT_NODE_ID);

		root = configDAO.getFolder(Node.ROOT_NODE_ID);
		folder1 = configDAO.getFolder(folder1.getId());

		// After move the target's last_modified should have been updated
		assertTrue(root.getLastModified().getTime() > lastModifiedOfTarget.getTime());

		// After move the source's last_modified should have been updated
		assertTrue(folder1.getLastModified().getTime() > lastModifiedOfSource.getTime());

		root = configDAO.getFolder(Node.ROOT_NODE_ID);

		// After move root node has two child nodes
		assertEquals(2, root.getChildNodes().size());

		folder1 = configDAO.getFolder(folder1.getId());
		// After mode SomeFolder has no child nodes
		assertTrue(folder1.getChildNodes().isEmpty());

	}

	@Test
	@FlywayTest(invokeCleanDB = true)
	public void testUpdateConfig() {

		Folder root = configDAO.getFolder(Node.ROOT_NODE_ID);

		Folder folder1 = configDAO.createFolder(Folder.builder().name("SomeFolder").parent(root).build());

		ConfigPv configPv1 = ConfigPv.builder().pvName("configPv1").build();
		ConfigPv configPv2 = ConfigPv.builder().pvName("configPv2").build();

		Config config = Config.builder().active(true).name("My config").parent(folder1).description("description")
				.system("system").configPvList(Arrays.asList(configPv1, configPv2)).build();

		config = configDAO.createConfiguration(config);
		
		Date lastModified = config.getLastModified();

		SnapshotPv<Integer> snapshotPv1 = SnapshotPv.<Integer>builder().dtype(1).fetchStatus(true).severity(2).status(3)
				.time(1000L).timens(20000).value(10).configPv(config.getConfigPvList().get(0)).build();

		SnapshotPv<Integer> snapshotPv2 = SnapshotPv.<Integer>builder().dtype(1).fetchStatus(true).severity(2).status(3)
				.time(1000L).timens(20000).value(20).configPv(config.getConfigPvList().get(1)).build();

		Snapshot snapshot = Snapshot.builder().approve(true).configId(config.getId())
				.snapshotPvList(Arrays.asList(snapshotPv1, snapshotPv2)).build();

		snapshot = configDAO.savePreliminarySnapshot(snapshot);

		assertEquals(10, snapshot.getSnapshotPvList().get(0).getValue());
		assertEquals(20, snapshot.getSnapshotPvList().get(1).getValue());

		Snapshot fullSnapshot = snapshotDAO.getSnapshot(snapshot.getId(), true);

		assertNull(fullSnapshot);

		List<Snapshot> snapshots = snapshotDAO.getSnapshots(config.getId());

		assertTrue(snapshots.isEmpty());

		snapshotDAO.commitSnapshot(snapshot.getId(), "user", "comment");

		fullSnapshot = snapshotDAO.getSnapshot(snapshot.getId(), true);

		assertNotNull(fullSnapshot);
		assertEquals(2, fullSnapshot.getSnapshotPvList().size());

		Config updatedConfig = Config.builder().id(config.getId()).active(true).name("My updated config")
				.parent(folder1).description("Updated description").system("Updated system")
				.configPvList(Arrays.asList(configPv1)).build();
		

		updatedConfig = configDAO.updateConfiguration(updatedConfig);

		assertEquals("My updated config", updatedConfig.getName());
		
		// Verify that last modified time has been updated
		assertTrue(updatedConfig.getLastModified().getTime() > lastModified.getTime());

	}

	@Test
	@FlywayTest(invokeCleanDB = true)
	public void testDeleteRootNode() {

		// Try to delete root folder (id = 1)
		configDAO.deleteNode(Node.ROOT_NODE_ID);

		// Root folder still there!
		assertNotNull(configDAO.getFolder(Node.ROOT_NODE_ID));

	}

	@Test(expected = NodeNotFoundException.class)
	public void testGetConfigThatIsNotAConfig() {

		configDAO.getConfiguration(Node.ROOT_NODE_ID);

	}

	@Test(expected = NodeNotFoundException.class)
	@FlywayTest(invokeCleanDB = true)
	public void testGetFolderThatIsNotAFolder() {

		Folder root = configDAO.getFolder(Node.ROOT_NODE_ID);

		Folder folder1 = configDAO.createFolder(Folder.builder().name("SomeFolder").parent(root).build());

		ConfigPv configPv1 = ConfigPv.builder().pvName("configPv1").build();

		Config config = Config.builder().active(true).name("My config").parent(folder1).description("description")
				.system("system").configPvList(Arrays.asList(configPv1)).build();

		config = configDAO.createConfiguration(config);

		configDAO.getFolder(config.getId());

	}

	@Test
	public void testNameClash() {

		Node n1 = Folder.builder().name("n1").build();
		Node n2 = Folder.builder().name("n1").build();
		Node n3 = Folder.builder().name("n3").build();

		ConfigJdbcDAO jdbcDAO = (ConfigJdbcDAO) configDAO;

		assertTrue(jdbcDAO.doesNameClash(n1, Arrays.asList(n2, n3)));
		assertFalse(jdbcDAO.doesNameClash(n1, Arrays.asList(n3)));

		Node n4 = Config.builder().name("n1").build();

		assertFalse(jdbcDAO.doesNameClash(n1, Arrays.asList(n4)));
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testRenameRootFolder() {
		configDAO.renameNode(Node.ROOT_NODE_ID, null);
	}
	
	@Test(expected = IllegalArgumentException.class)
	@FlywayTest(invokeCleanDB = true)
	public void testRenameFolderNameAlreadyExists() {
		
		Folder root = configDAO.getFolder(Node.ROOT_NODE_ID);
		
		configDAO.createFolder(Folder.builder().name("Folder1").parent(root).build());
		
		Folder folder2 = configDAO.createFolder(Folder.builder().name("Folder2").parent(root).build());
		
		configDAO.renameNode(folder2.getId(), "Folder1");
	}
	
	@Test(expected = IllegalArgumentException.class)
	@FlywayTest(invokeCleanDB = true)
	public void testRenameConfigNameAlreadyExists() {
		
		Folder root = configDAO.getFolder(Node.ROOT_NODE_ID);
		
		configDAO.createConfiguration(Config.builder().description("description").name("Config1").parent(root).build());
		
		Config config2 = configDAO.createConfiguration(Config.builder().description("description").name("Config2").parent(root).build());
		
		configDAO.renameNode(config2.getId(), "Config1");
	}
	
	@Test
	@FlywayTest(invokeCleanDB = true)
	public void testRenameFolder(){
		
		Folder root = configDAO.getFolder(Node.ROOT_NODE_ID);
		
		Folder folder1 = configDAO.createFolder(Folder.builder().name("Folder1").parent(root).build());
		
		Date lastModified = folder1.getLastModified();
		
		configDAO.createConfiguration(Config.builder().description("whatever").name("Config1").parent(root).build());
		
		configDAO.renameNode(folder1.getId(), "NewName");
		
		folder1 = configDAO.getFolder(folder1.getId());
		
		assertTrue(folder1.getLastModified().getTime() > lastModified.getTime());
	}

}
