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
package se.esss.ics.masar.persistence.dao.impl;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import se.esss.ics.masar.model.Snapshot;

public class SnapshotRowMapper implements RowMapper<Snapshot> {

	@Override
	public Snapshot mapRow(ResultSet resultSet, int rowIndex) throws SQLException {
			
		return Snapshot.builder()
				.approve(resultSet.getBoolean("approve"))
				.configId(resultSet.getInt("config_id"))
				.created(resultSet.getTimestamp("created"))
				.id(resultSet.getInt("id"))
				.userName(resultSet.getString("name"))
				.comment(resultSet.getString("comment"))
				.build();
	}
}
