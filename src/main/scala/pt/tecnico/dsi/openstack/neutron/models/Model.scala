package pt.tecnico.dsi.openstack.neutron.models

import pt.tecnico.dsi.openstack.common.models.Identifiable

trait Model {
  type Create
  type Read <: Identifiable
  type Update
}
