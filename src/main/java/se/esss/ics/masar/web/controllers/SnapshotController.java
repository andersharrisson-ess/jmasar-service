/** 
 * Copyright (C) ${year} European Spallation Source ERIC.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package se.esss.ics.masar.web.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.ApiOperation;
import se.esss.ics.masar.model.Snapshot;
import se.esss.ics.masar.services.IServices;

@RestController
public class SnapshotController extends BaseController {

	@Autowired
	private IServices services;

	@ApiOperation(value = "Take a snapshot, i.e. save preliminary.")
	@PutMapping("/snapshot/{configId}")
	public Snapshot takeSnapshot(@PathVariable int configId) {
		return services.takeSnapshot(configId);
	}

	@ApiOperation(value = "Get a snapshot, including its values.", consumes = JSON)
	@GetMapping("/snapshot/{snapshotId}")
	public Snapshot getSnapshot(@PathVariable int snapshotId) {

		return services.getSnapshot(snapshotId);
	}

	@ApiOperation(value = "Delete a snapshot", consumes = JSON)
	@DeleteMapping("/snapshot/{snapshotId}")
	public void deleteSnapshot(@PathVariable int snapshotId) {

		services.deleteSnapshot(snapshotId);
	}

	@ApiOperation(value = "Commit a snapshot, i.e. update with user name and comment.")
	@PostMapping("/snapshot/{snapshotId}")
	public Snapshot commitSnapshot(@PathVariable int snapshotId, @RequestParam(required = true) String userName,
			@RequestParam(required = true) String comment) {

		return services.commitSnapshot(snapshotId, userName, comment);
	}
}
