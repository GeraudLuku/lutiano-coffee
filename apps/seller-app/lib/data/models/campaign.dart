import 'package:lutiano_core/lutiano_core.dart';
import 'targets.dart';

/// One tier of a campaign: reach [thresholdXaf] of net sales to earn [bonusXaf].
class CampaignTier {
  const CampaignTier({required this.thresholdXaf, required this.bonusXaf});

  factory CampaignTier.fromJson(Map<String, dynamic> j) => CampaignTier(
      thresholdXaf: asInt(j['threshold_xaf']), bonusXaf: asInt(j['bonus_xaf']));

  final int thresholdXaf, bonusXaf;
}

/// A sales campaign that reaches the seller, with their own progress. [tiers] come with the single-campaign read only.
class Campaign {
  const Campaign({
    required this.id,
    required this.name,
    required this.startsOn,
    required this.endsOn,
    required this.state,
    required this.salesXaf,
    required this.bonusXaf,
    this.description,
    this.operationName,
    this.nextThresholdXaf,
    this.nextBonusXaf,
    this.paidBonusXaf,
    this.tiers = const [],
  });

  factory Campaign.fromJson(Map<String, dynamic> j) => Campaign(
        id: asString(j['id']),
        name: asString(j['name']),
        description: asStringOrNull(j['description']),
        operationName: asStringOrNull(j['operation_name']),
        startsOn: DateTime.parse(asString(j['starts_on'])),
        endsOn: DateTime.parse(asString(j['ends_on'])),
        state: TargetState.fromApi(asString(j['state'])),
        salesXaf: asInt(j['sales_xaf']),
        bonusXaf: asInt(j['bonus_xaf']),
        nextThresholdXaf: asIntOrNull(j['next_threshold_xaf']),
        nextBonusXaf: asIntOrNull(j['next_bonus_xaf']),
        paidBonusXaf: asIntOrNull(j['paid_bonus']),
        tiers: asList(j['tiers']).map(CampaignTier.fromJson).toList(),
      );

  final String id, name;
  final String? description, operationName;
  final DateTime startsOn, endsOn;
  final TargetState state;

  /// The seller's net sales in the campaign so far, and the bonus they would get now.
  final int salesXaf, bonusXaf;

  /// The next tier up (null once the top one is reached), and the bonus paid when the campaign was settled (null before).
  final int? nextThresholdXaf, nextBonusXaf, paidBonusXaf;
  final List<CampaignTier> tiers;

  /// Progress toward the next tier, 0 to 100 (100 once every tier is reached).
  double get progressPct => nextThresholdXaf == null
      ? 100
      : (salesXaf * 100 / nextThresholdXaf!).clamp(0, 100).toDouble();
}
