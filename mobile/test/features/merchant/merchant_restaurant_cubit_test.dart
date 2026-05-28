import 'package:flutter_test/flutter_test.dart';
import 'package:foodya_mobile/features/merchant/domain/models/merchant_restaurant_request.dart';
import 'package:foodya_mobile/features/merchant/presentation/cubit/merchant_restaurant_cubit.dart';
import 'package:foodya_mobile/features/merchant/presentation/cubit/merchant_restaurant_state.dart';
import 'package:mocktail/mocktail.dart';

import '../../helpers/mock_repositories.dart';
import '../../helpers/test_models.dart';

class _FakeMerchantRestaurantRequest extends Fake
    implements MerchantRestaurantRequest {}

void main() {
  late MockMerchantRestaurantRepository repository;

  setUpAll(() {
    registerFallbackValue(_FakeMerchantRestaurantRequest());
  });

  setUp(() {
    repository = MockMerchantRestaurantRepository();
  });

  test('loads empty and success restaurant states', () async {
    when(() => repository.listRestaurants()).thenAnswer((_) async => []);
    final cubit = MerchantRestaurantCubit(repository: repository);

    await cubit.loadRestaurants();

    expect(cubit.state.status, MerchantRestaurantStatus.success);
    expect(cubit.state.restaurants, isEmpty);

    when(
      () => repository.listRestaurants(),
    ).thenAnswer((_) async => [merchantRestaurant()]);

    await cubit.loadRestaurants();

    expect(cubit.state.restaurant?.name, 'Pho House');
  });

  test('creates and updates restaurant', () async {
    when(
      () => repository.createRestaurant(any()),
    ).thenAnswer((_) async => merchantRestaurant());
    when(
      () => repository.updateRestaurant(
        restaurantId: any(named: 'restaurantId'),
        request: any(named: 'request'),
      ),
    ).thenAnswer((_) async => merchantRestaurant(open: false));
    final cubit = MerchantRestaurantCubit(repository: repository);

    await cubit.create(merchantRestaurantRequest());
    await cubit.update(
      restaurantId: 'restaurant-1',
      request: merchantRestaurantRequest(isOpen: false),
    );

    expect(cubit.state.status, MerchantRestaurantStatus.success);
    expect(cubit.state.restaurant?.open, isFalse);
    expect(cubit.state.infoMessage, 'Restaurant updated.');
  });

  test('toggles open state through update request', () async {
    when(
      () => repository.updateRestaurant(
        restaurantId: any(named: 'restaurantId'),
        request: any(named: 'request'),
      ),
    ).thenAnswer((_) async => merchantRestaurant(open: false));
    final cubit = MerchantRestaurantCubit(repository: repository);

    await cubit.setOpen(
      restaurantId: 'restaurant-1',
      request: merchantRestaurantRequest(),
      isOpen: false,
    );

    final captured =
        verify(
          () => repository.updateRestaurant(
            restaurantId: 'restaurant-1',
            request: captureAny(named: 'request'),
          ),
        ).captured.single;
    expect(captured.isOpen, isFalse);
    expect(cubit.state.restaurant?.open, isFalse);
  });

  test('exposes failure state for load and save failures', () async {
    when(() => repository.listRestaurants()).thenThrow(Exception('load'));
    final cubit = MerchantRestaurantCubit(repository: repository);

    await cubit.loadRestaurants();

    expect(cubit.state.status, MerchantRestaurantStatus.failure);
    expect(cubit.state.errorMessage, isNotEmpty);

    when(() => repository.createRestaurant(any())).thenThrow(Exception('save'));

    await cubit.create(merchantRestaurantRequest());

    expect(cubit.state.status, MerchantRestaurantStatus.failure);
    expect(cubit.state.errorMessage, isNotEmpty);
  });
}
