package ch.epfl.structure
case class StructureIvano(
    uuid: String,
    cell: Seq[Seq[Double]],
    cellAngles: Seq[Double],
    cellLengths: Seq[Double],
    cellVolume: Double,
    sites: Seq[SiteIvano],
    pbc: Seq[Boolean])


//case class SiteIvano(position: Seq[Double], kindName: String)
case class SiteIvano(position: Seq[Double], kindName: String, properties: PropertiesIvano)

case class PropertiesIvano(weights: Seq[Double], mass: Double)
//case class PropertiesIvano(weights: Seq[Double])

