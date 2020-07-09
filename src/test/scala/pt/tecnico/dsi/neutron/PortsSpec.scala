package pt.tecnico.dsi.neutron

import pt.tecnico.dsi.neutron.models.Port

class PortsSpec extends Utils {

  object stub {
    val create: Port.Create = Port.Create(
     networkId = "test"
    )
    val update: Port.Update = Port.Update(
      name = Some("port-name")
    )
  }

  "Ports service" should {
    "should create" in {
      for {
        neutron <- client
        _ <- neutron.ports.create(stub.create)
      } yield assert {
        true
      }
    }
    "delete" in {
      for {
        neutron <- client
        port <- neutron.ports.create(stub.create)
        _ <- neutron.ports.delete(port.id)
      } yield assert {
        true
      }
    }
    "list" in {
      for {
        neutron <- client
        _ <- neutron.ports.list().compile.toList
      } yield assert {
        true
      }
    }
    "update" in {
      for {
        neutron <- client
        port <- neutron.ports.create(stub.create)
        updated <- neutron.ports.update(port.id, stub.update)
      } yield assert {
        updated.name == stub.update.name.get
      }
    }
  }
}
