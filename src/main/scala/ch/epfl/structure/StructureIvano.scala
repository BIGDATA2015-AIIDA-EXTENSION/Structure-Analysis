package ch.epfl.structure
case class StructureIvano(
    uuid: String,
    cell: Seq[Seq[Double]],
    cellVolume: Double,
    sites: Seq[SiteIvano],
    pbc: Seq[Boolean])


case class SiteIvano(position: Seq[Double], kindName: String)

