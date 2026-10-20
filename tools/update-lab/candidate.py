"""CP1 fixture boundary around the upstream TUF client; not an application installer.

All cryptographic metadata verification is done by python-tuf, never a Suite serializer.
Product/channel/data compatibility are authenticated target custom fields.
"""
from dataclasses import dataclass


class CandidateRejected(ValueError):
    pass


@dataclass(frozen=True)
class ExpectedIdentity:
    product: str
    platform: str
    channel: str
    installed_version: int
    data_reader: int


def select_candidate(updater, target_name, expected):
    info = updater.get_targetinfo(target_name)  # Upstream validates root/roles/signatures/freshness first.
    if info is None:
        raise CandidateRejected("Target is not authenticated by the repository")
    custom = info.unrecognized_fields.get("custom", {})
    for field in ("product", "platform", "channel"):
        if custom.get(field) != getattr(expected, field):
            raise CandidateRejected("Wrong " + field)
    for field in ("version", "minimum_reader", "maximum_reader"):
        if type(custom.get(field)) is not int:
            raise CandidateRejected("Missing or unsupported compatibility metadata")
    if custom["version"] <= expected.installed_version:
        raise CandidateRejected("Target is not newer than the installed application")
    if not custom["minimum_reader"] <= expected.data_reader <= custom["maximum_reader"]:
        raise CandidateRejected("Existing data is outside the target's declared reader range")
    return info
